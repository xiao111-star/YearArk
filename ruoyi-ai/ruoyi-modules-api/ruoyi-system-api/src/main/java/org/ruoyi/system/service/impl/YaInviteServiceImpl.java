package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.YaInviteToken;
import org.ruoyi.system.domain.dto.YaInviteDto;
import org.ruoyi.system.domain.dto.YaInviteQueryDto;
import org.ruoyi.system.domain.vo.YaInviteVo;
import org.ruoyi.system.mapper.YaInviteMapper;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaInviteService;
import org.ruoyi.system.service.IYaInviteTokenService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class YaInviteServiceImpl extends ServiceImpl<YaInviteMapper, YaInvite> implements IYaInviteService {

    private final IYaAlbumService albumService;
    private final IYaInviteTokenService inviteTokenService;

    public YaInviteServiceImpl(IYaAlbumService albumService, @Lazy IYaInviteTokenService inviteTokenService) {
        this.albumService = albumService;
        this.inviteTokenService = inviteTokenService;
    }

    @Override
    public TableDataInfo<YaInviteVo> queryPage(YaInviteQueryDto query, PageQuery pageQuery) {
        Page<YaInvite> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaInviteVo.class), page.getTotal());
    }

    @Override
    public List<YaInviteVo> queryList(YaInviteQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaInviteVo.class);
    }

    @Override
    public YaInviteVo queryById(Integer id) {
        YaInvite entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaInviteVo.class);
    }

    @Override
    public boolean insertByDto(YaInviteDto dto) {
        YaInvite yaInvite = BeanUtil.toBean(dto, YaInvite.class);
        yaInvite.setStatus(dto.getStatus()!=null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        yaInvite.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaInvite);
    }

    @Override
    public boolean updateByDto(YaInviteDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaInvite.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        for (Integer id : ids) {
            if (this.getById(id) == null) {
                throw new ServiceException("邀请记录 [id=" + id + "] 不存在");
            }
        }
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaInvite> buildWrapper(YaInviteQueryDto q) {
        LambdaQueryWrapper<YaInvite> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getAlbumId() != null, YaInvite::getAlbumId, q.getAlbumId());
        qw.eq(q.getStatus() != null, YaInvite::getStatus, q.getStatus());
        qw.orderByDesc(YaInvite::getCreateAt);
        return qw;
    }

    @Override
    public boolean createInvite(YaInviteDto dto, Integer userId) {
        albumService.checkOwnership(dto.getAlbumId(), userId);

        YaInvite invite = new YaInvite();
        invite.setAlbumId(dto.getAlbumId());
        invite.setAccessCode(dto.getAccessCode());
        invite.setInviteCode(generateUniqueCode());
        invite.setStatus(CommonConstants.IS_AVAILABLE);
        invite.setExpireAt(dto.getExpireAt());
        invite.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(invite);
    }

    @Override
    public boolean disableInvite(Integer id, Integer userId) {
        YaInvite invite = this.getById(id);
        if (invite == null) {
            throw new ServiceException("邀请链接不存在");
        }
        albumService.checkOwnership(invite.getAlbumId(), userId);

        YaInvite update = new YaInvite();
        update.setId(id);
        update.setStatus(CommonConstants.NOT_AVAILABLE);
        boolean result = this.updateById(update);
        
        // 禁用成功后，将该邀请链接关联的所有 token 加入黑名单
        if (result) {
            addTokensToBlacklist(id);
        }
        
        return result;
    }

    /**
     * 将邀请链接关联的所有 token 加入 Redis 黑名单
     */
    private void addTokensToBlacklist(Integer inviteId) {
        // 查询该邀请链接下的所有 token
        List<YaInviteToken> tokens = inviteTokenService.list(
            new LambdaQueryWrapper<YaInviteToken>()
                .eq(YaInviteToken::getInviteId, inviteId)
                .eq(YaInviteToken::getStatus, CommonConstants.IS_AVAILABLE)
        );
        
        // 将每个 token 加入黑名单，设置过期时间为 30 天
        for (YaInviteToken token : tokens) {
            if (token.getToken() != null && !token.getToken().isEmpty() && !"pending".equals(token.getToken())) {
                String blacklistKey = "invite:token:blacklist:" + token.getToken();
                RedisUtils.setCacheObject(blacklistKey, true, Duration.ofDays(30));
            }
        }
        
        // 同时禁用数据库中的 token 记录
        if (!tokens.isEmpty()) {
            List<Integer> tokenIds = tokens.stream()
                .map(YaInviteToken::getId)
                .collect(java.util.stream.Collectors.toList());
            inviteTokenService.lambdaUpdate()
                .in(YaInviteToken::getId, tokenIds)
                .set(YaInviteToken::getStatus, CommonConstants.NOT_AVAILABLE)
                .update();
        }
    }

    /**
     * 生成 6 位唯一邀请码
     */
    private String generateUniqueCode() {
        String code;
        do {
            code = RandomUtil.randomString(6);
        } while (this.count(
            new LambdaQueryWrapper<YaInvite>().eq(YaInvite::getInviteCode, code)) > 0);
        return code;
    }
}
