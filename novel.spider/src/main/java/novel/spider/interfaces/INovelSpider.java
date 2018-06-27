package novel.spider.interfaces;

import java.util.Iterator;
import java.util.List;

import novel.spider.entitys.Novel;

/**
 * 爬取某个站点的小说列表
 */
public interface INovelSpider {
	/** 抓取某一个页面时最大的尝试次数3 */
	public static final int MAX_TRY_TIMES = 3;
	/**
	 * 根据url返回一堆小说
	 * @param url
	 * @param maxTryTimes 网页下载的最大次数（允许失败重试的次数）
	 * @return
	 */
	public List<Novel> getsNovel(String url, Integer maxTryTimes) throws Exception;
	
	public boolean hasNext();
	public String next();
	public Iterator<List<Novel>> iterator(String firstPage, Integer maxTryTimes);
	
}
