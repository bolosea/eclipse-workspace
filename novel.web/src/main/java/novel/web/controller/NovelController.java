package novel.web.controller;

import com.alibaba.fastjson.JSONObject;
import novel.spider.entitys.Chapter;
import novel.spider.entitys.ChapterDetail;
import novel.spider.entitys.Novel;
import novel.spider.impl.download.NovelDownload;
import novel.spider.interfaces.INovelDownload;
import novel.spider.util.DownloadConfigContext;
import novel.spider.util.FileUtil;
import novel.web.annotation.Auth;
import novel.web.constants.Constants;
import novel.web.entitys.JSONResponse;
import novel.web.entitys.Page;
import novel.web.entitys.User;
import novel.web.service.NovelService;
import novel.web.service.UserService;
import novel.web.utils.Base64Util;
import novel.web.utils.CookieUtil;
import novel.web.utils.RedisUtil;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("novel")
public class NovelController {
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    @Autowired
    NovelService novelService;

    @Autowired
    UserService userService;

    @Autowired
    RedisUtil redisUtil;

    /**
     * 下载小说
     *
     * @param base64Url
     * @param name
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/download")
    public ResponseEntity<byte[]> download(@RequestParam("base64Url") String base64Url, String name) throws IOException {
        //下载过的文件路径
        Map<Object, Object> downloadpath = redisUtil.hmget("downloadpath");
        String localsavepath = "";
        if (downloadpath == null || downloadpath.get(name) == null) {
            Map<String, Object> path = new HashMap<>();
            String url = Base64Util.decode(base64Url);
            INovelDownload novelDownload = new NovelDownload();
            localsavepath = novelDownload.download(url, DownloadConfigContext.configuration);
            path.put(name, localsavepath);
            redisUtil.hmset("downloadpath", path);
        } else {
            localsavepath = (String) downloadpath.get(name);
        }
        File file = new File(localsavepath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", new String((name + ".txt").getBytes("utf-8"), "iso-8859-1"));
        headers.setContentType(MediaType.TEXT_PLAIN);
        //据说这边用HttpStatus.CREATED 电脑端可以下载但是手机端不行，要换成HttpStatus.OK
        return new ResponseEntity<byte[]>(FileUtil.toByteArray(file),
                headers, HttpStatus.OK);
    }

    /**
     * 根据关键词查找小说
     *
     * @param keyword
     * @return
     */
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    @ResponseBody
    public JSONResponse search(String keyword) {
        List<Novel> novels = novelService.getsNovelByKeyword(keyword);
        return JSONResponse.success(novels);
    }

    /**
     * 根据关键词分页查找小说
     *
     * @param keyword
     * @return
     */
    @RequestMapping(value = "/searchByPage", method = RequestMethod.POST)
    @ResponseBody
    public JSONResponse searchByPage(String keyword, int currentPage, int pageSize) {
        logger.info("keyword:" + keyword);
        Page<Novel> page = novelService.getNovelByPage(keyword, currentPage, pageSize);
        return JSONResponse.success(page);
    }


    /**
     * 分页获取所有小说
     *
     * @param currPage 页码
     * @param pageSize 每页显示条数
     * @return
     */
    @RequestMapping(value = "/getAllNovelByPage", method = RequestMethod.GET)
    @ResponseBody
    public JSONResponse getAllNovelByPage(String keyword, int currPage, int pageSize) {
        Page<Novel> pages = novelService.getAllNovelByPage(keyword, currPage, pageSize);
        return JSONResponse.success(pages.getPages(), pages.getTotalCount());
    }

    /**
     * 根据base64编码过的url查找小说章节
     *
     * @param base64Url
     * @return
     */
    @RequestMapping(value = "/getChapters", method = RequestMethod.GET)
    public ModelAndView getChapters(String base64Url) {
        List<Chapter> chapters = null;
        boolean isSuccess = false;
        try {
            chapters = novelService.getChapters(base64Url);
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        ModelAndView view = new ModelAndView();
        view.setViewName("novel/chapterList");
        view.addObject("chapters", chapters);
        view.addObject("chapterBase64Url", base64Url);
        view.addObject("isSuccess", isSuccess);
        //为了freemarker能调用base64util加密方法
        view.addObject("Base64Util", new Base64Util());
        return view;
    }

    /**
     * 根据章节url查找对应章节内容
     *
     * @param url              章节url
     * @param chapterBase64Url 返回章节列表
     * @return
     */
    @RequestMapping("getChapterDetail")
    public ModelAndView getChapterDetail(HttpServletRequest request, HttpServletResponse response, String url, String chapterBase64Url) {
        boolean isSuccess = false;
        ChapterDetail chapterDetail = null;
        //将前台传来的章节详情url解码
        url = Base64Util.decode(url);
        try {
            logger.info("url = " + url);
            chapterDetail = novelService.getChapterDetail(url);
            chapterDetail.setPrev(Base64Util.encode(chapterDetail.getPrev()));
            chapterDetail.setNext(Base64Util.encode(chapterDetail.getNext()));
            Cookie rc = CookieUtil.getCookie(request.getCookies(), "rc");
            String readToken = "";
            if (null == rc) {
                readToken = UUID.randomUUID().toString().replace("-", "");
                //客户端存储一个cookie用来跟redis比较
                Cookie readCookie = new Cookie("rc", readToken);
                readCookie.setPath("/");
                readCookie.setMaxAge(Constants.DEFAULT_EXPIRES_HOUR);
                response.addCookie(readCookie);
            } else {
                readToken = rc.getValue();
            }
            //将章节阅读记录存在redis中
            redisUtil.del(readToken);
            redisUtil.lSet(readToken, Base64Util.encode(url), Constants.DEFAULT_EXPIRES_HOUR);
            redisUtil.lSet(readToken, Base64Util.encode(chapterDetail.getTitle()), Constants.DEFAULT_EXPIRES_HOUR);
            User user = (User) request.getSession().getAttribute(Constants.CURRENT_USER);
            if (user != null) {
                //2018 0630 注释 不需要这个字段
                //user.setLastReadChapterDetailUrl(url);
                //user.setLastReadChapterTitle(chapterDetail.getTitle());
                userService.update(user);
            }
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        ModelAndView view = new ModelAndView();
        view.setViewName("novel/chapterDetail");
        view.addObject("chapterDetail", chapterDetail);
        view.addObject("isSuccess", isSuccess);
        view.addObject("chapterBase64Url", chapterBase64Url);
        return view;
    }

    /**
     * 根据销售小说id删除小说
     *
     * @param param
     * @return
     */
    @RequestMapping(value = "/deleteNovelById", method = RequestMethod.POST)
    @ResponseBody
    @Auth
    public JSONResponse deleteNovelById(@RequestBody String param) {
        JSONObject parse = JSONObject.parseObject(param);
        long id = parse.getLong("id");
        int i = novelService.deleteNovelById(id);
        if (i == 1) {
            return JSONResponse.success(null, "删除成功");
        }
        return JSONResponse.error("删除失败");
    }

    /**
     * 根据小说id修改对应小说的某个字段值
     *
     * @param id    小说id
     * @param field 字段名
     * @param value 修改后的值
     * @return
     */
    @RequestMapping(value = "/updateNovelById", method = RequestMethod.POST)
    @ResponseBody
    @Auth
    public JSONResponse updateNovelById(long id, String field, String value) {
        novelService.updateNovelById(id, field, value);
        return JSONResponse.success(null);
    }

    /**
     * 根据关键词搜索相似小说或者作者对应的小说
     *
     * @param keyword:关键词
     * @return
     */
    @RequestMapping(value = "/searchLikeByKey", method = RequestMethod.POST)
    @ResponseBody
    public JSONResponse searchLikeByKey(@RequestParam("keyword") String keyword) {
        List<Object> objects = redisUtil.lGet(keyword, 0, redisUtil.lGetListSize(keyword));
        if (objects == null || objects.size() == 0) {
            List<String> names = novelService.searchLikeByKey(keyword);
            if (names != null && names.size() > 0) {
                redisUtil.lSet(keyword, names, Constants.DEFAULT_EXPIRES_HOUR);
                return JSONResponse.success(names);
            }
            return JSONResponse.error("no novel!");
        }
        //真坑,这个objects返回的是[[xxx,xxx]]
        return JSONResponse.success(objects.get(0));

    }


}
