package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.EditablePageVo;
import org.ruoyi.system.domain.vo.YaAlbumVo;
import org.ruoyi.system.service.AlbumPageEditService;
import org.ruoyi.system.service.IYaAlbumService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公开接口 - 首页公开纪念册展示（无需登录）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/album")
public class PublicAlbumController extends BaseController {

    private final IYaAlbumService albumService;
    private final AlbumPageEditService albumPageEditService;

    /**
     * 公开纪念册列表（is_public = 1）
     */
    @GetMapping("/list")
    public R<List<YaAlbumVo>> publicList() {
        YaAlbumQueryDto query = new YaAlbumQueryDto();
        query.setIsPublic(1);
        return R.ok(albumService.queryList(query));
    }

    /**
     * 公开查看纪念册（分享链接，无需登录）
     * 返回纪念册基本信息 + 所有页面编辑数据（前端渲染）
     */
    @GetMapping("/{id}/view")
    public R<ShareAlbumViewVo> publicView(@PathVariable Integer id) {
        YaAlbum album = albumService.getById(id);
        if (album == null) {
            throw new ServiceException("纪念册不存在");
        }
        List<EditablePageVo> pages = albumPageEditService.getEditData(id);

        ShareAlbumViewVo vo = new ShareAlbumViewVo();
        vo.setName(album.getName());
        vo.setDes(album.getDes());
        vo.setPages(pages);
        return R.ok(vo);
    }

    /**
     * 分享查看返回的 VO（内部类，简单场景不单独建文件）
     */
    @lombok.Data
    public static class ShareAlbumViewVo {
        private String name;
        private String des;
        private List<EditablePageVo> pages;
    }
}
