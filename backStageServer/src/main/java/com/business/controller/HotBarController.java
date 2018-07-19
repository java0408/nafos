package com.business.controller;

import com.business.entry.BackStageGameTypes;
import com.business.entry.BackStageGames;
import com.business.service.HotBarService;
import com.business.tools.ResponseTool;
import com.hxy.nettygo.result.base.annotation.Nuri;
import com.hxy.nettygo.result.base.annotation.Route;
import com.hxy.nettygo.result.base.tools.CastUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route
@Nuri(uri = "/hotBar")
public class HotBarController {
    @Autowired
    HotBarService hotBarService;

    @Nuri(uri = "/getBanners", method = "GET", type = "JSON")
    public Object getBanners(Map<String, Object> map) {
        List<Map> banners = hotBarService.selectAllBannerList();
        return ResponseTool.newObjectResponse(banners);
    }

    @Nuri(uri = "/updateBanner", method = "POST", type = "JSON")
    public Object updateBanner(Map<String, Object> map) {
        String id = map.get("id").toString();
        String gameName = map.get("gameName").toString();
        String appID = map.get("appID").toString();
        String imgUrl = map.get("imgUrl").toString();
        int order = CastUtil.castInt(map.get("order"));
        int count = hotBarService.updateBanner(id, gameName, appID, imgUrl, order);
        if (count > 0) {
            return ResponseTool.newObjectResponse(count);
        }
        return ResponseTool.newErrorResponse("更新失败，请检查数据是否错误");
    }

    @Nuri(uri = "/deleteBanner", method = "POST", type = "JSON")
    public Object deleteBanner(Map<String, Object> map) {
        String id = map.get("id").toString();
        int count = hotBarService.deleteBanner(id);
        if (count > 0) {
            return ResponseTool.newObjectResponse(count);
        }
        return ResponseTool.newErrorResponse("删除失败，请检查数据是否错误");
    }

    @Nuri(uri = "/addBanner", method = "POST", type = "JSON")
    public Object addBanner(Map<String, Object> map) {
        String gameName = map.get("gameName").toString();
        String appID = map.get("appID").toString();
        String imgUrl = map.get("imgUrl").toString();
        int order = CastUtil.castInt(map.get("order"));
        int count = hotBarService.addBanner(gameName,appID,imgUrl,order);
        if (count > 0) {
            return ResponseTool.newObjectResponse(count);
        }
        return ResponseTool.newErrorResponse("插入新信息失败，请检查数据是否错误");
    }


    @Nuri(uri = "/getGames", method = "GET", type = "JSON")
    public Object getGames(Map<String, Object> map) {
        String type = map.get("gameType").toString();
        List<BackStageGames> games;
        if("所有类型".equals(type)){
            games = hotBarService.selectAllGameList();
        }else{
            games = hotBarService.selectGamesByType(type);
        }
        return ResponseTool.newObjectResponse(games);
    }

    @Nuri(uri = "/updateGame", method = "POST", type = "JSON")
    public Object updateGame(Map<String, Object> map) {
        String id = map.get("id").toString();
        String gameName = map.get("gameName").toString();
        String imgUrl = map.get("imgUrl").toString();
        String iconUrl = map.get("iconUrl").toString();
        String appID = map.get("appID").toString();
        int order = CastUtil.castInt(map.get("order"));
        String gameIntro = map.get("gameIntro").toString();
        int count = hotBarService.updateGame(id,gameName,imgUrl,iconUrl,appID,order,gameIntro);
        if (count > 0) {
            return ResponseTool.newObjectResponse(count);
        }
        return ResponseTool.newErrorResponse("更新失败，请检查数据是否错误");
    }

    @Nuri(uri = "/deleteGame", method = "POST", type = "JSON")
    public Object deleteGame(Map<String, Object> map) {
        String id = map.get("id").toString();
        int count = hotBarService.deleteGame(id);
        if (count > 0) {
            return ResponseTool.newObjectResponse(count);
        }
        return ResponseTool.newErrorResponse("删除失败，请检查数据是否错误");
    }

    @Nuri(uri = "/addGame", method = "POST", type = "JSON")
    public Object addGame(Map<String, Object> map) {
        String gameName = map.get("gameName").toString();
        String imgUrl = map.get("imgUrl").toString();
        String iconUrl = map.get("iconUrl").toString();
        String appID = map.get("appID").toString();
        int order = CastUtil.castInt(map.get("order"));
        String gameIntro = map.get("gameIntro").toString();
        int count = hotBarService.addGame(gameName,imgUrl,iconUrl,appID,order,gameIntro);
        if (count > 0) {
            return ResponseTool.newObjectResponse(count);
        }
        return ResponseTool.newErrorResponse("插入新信息失败，请检查数据是否错误");
    }


    @Nuri(uri = "/getAllTypes", method = "GET", type = "JSON")
    public Object getAllTypes(Map<String, Object> map) {
        List<BackStageGameTypes> games = hotBarService.selectAllGameTypes();
        return ResponseTool.newObjectResponse(games);
    }

    @Nuri(uri = "/getGameByType",method = "GET" ,type = "JSON")
    public Object getGameByType(Map<String,Object> map){
        return null;
    }
}
