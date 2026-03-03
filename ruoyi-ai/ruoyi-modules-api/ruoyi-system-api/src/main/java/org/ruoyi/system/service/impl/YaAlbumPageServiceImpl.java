package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.dto.YaAlbumPageDto;
import org.ruoyi.system.domain.dto.YaAlbumPageQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumPageVo;
import org.ruoyi.system.mapper.YaAlbumPageMapper;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaAlbumPageServiceImpl extends ServiceImpl<YaAlbumPageMapper, YaAlbumPage> implements IYaAlbumPageService {

    @Override
    public TableDataInfo<YaAlbumPageVo> queryPage(YaAlbumPageQueryDto query, PageQuery pageQuery) {
        Page<YaAlbumPage> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaAlbumPageVo.class), page.getTotal());
    }

    @Override
    public List<YaAlbumPageVo> queryList(YaAlbumPageQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaAlbumPageVo.class);
    }

    @Override
    public YaAlbumPageVo queryById(Integer id) {
        YaAlbumPage entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaAlbumPageVo.class);
    }

    @Override
    public boolean insertByDto(YaAlbumPageDto dto) {
        YaAlbumPage yaAlbumPage = BeanUtil.toBean(dto, YaAlbumPage.class);
        if (yaAlbumPage.getSort() == null){
            LambdaQueryWrapper<YaAlbumPage> qw = new LambdaQueryWrapper<>();
            qw.eq(YaAlbumPage::getAlbumId, yaAlbumPage.getAlbumId());
            yaAlbumPage.setSort((int) (this.count(qw) + 1));
        }
        yaAlbumPage.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaAlbumPage);
    }

    @Override
    public boolean updateByDto(YaAlbumPageDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaAlbumPage.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaAlbumPage> buildWrapper(YaAlbumPageQueryDto q) {
        LambdaQueryWrapper<YaAlbumPage> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getAlbumId() != null, YaAlbumPage::getAlbumId, q.getAlbumId());
        qw.orderByAsc(YaAlbumPage::getSort);
        return qw;
    }
}
