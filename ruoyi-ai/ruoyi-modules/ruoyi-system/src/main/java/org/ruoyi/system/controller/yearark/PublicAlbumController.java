package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumVo;
import org.ruoyi.system.service.IYaAlbumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开接口 - 首页公开纪念册展示（无需登录）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/album")
public class PublicAlbumController extends BaseController {

    private final IYaAlbumService albumService;

    /**
     * 公开纪念册列表（is_public = 1）
     */
    @GetMapping("/list")
    public R<List<YaAlbumVo>> publicList() {
        YaAlbumQueryDto query = new YaAlbumQueryDto();
        query.setIsPublic(1);
        return R.ok(albumService.queryList(query));
    }
}
