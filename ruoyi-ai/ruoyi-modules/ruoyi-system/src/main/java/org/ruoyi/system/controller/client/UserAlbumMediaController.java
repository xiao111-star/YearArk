package org.ruoyi.system.controller.client;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.YaAlbumMediaQueryDto;
import org.ruoyi.system.domain.vo.MediaStatsVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.util.YaLoginHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 素材查看接口
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/media")
public class UserAlbumMediaController extends BaseController {

    private final IYaAlbumMediaService mediaService;
    private final IYaAlbumService albumService;

    @GetMapping("/list")
    public R<List<YaAlbumMediaVo>> list(YaAlbumMediaQueryDto query) {
        albumService.checkOwnership(query.getAlbumId(), YaLoginHelper.getUserId());
        return R.ok(mediaService.queryList(query));
    }

    @GetMapping("/stats")
    public R<MediaStatsVo> stats(@RequestParam Integer albumId) {
        albumService.checkOwnership(albumId, YaLoginHelper.getUserId());
        return R.ok(mediaService.getStats(albumId));
    }
}
