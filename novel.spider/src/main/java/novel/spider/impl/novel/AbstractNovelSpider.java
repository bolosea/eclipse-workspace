package novel.spider.impl.novel;

import novel.spider.NovelSiteEnum;
import novel.spider.entitys.Novel;
import novel.spider.impl.AbstractSpider;
import novel.spider.interfaces.INovelSpider;
import novel.spider.util.NovelSpiderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 一个抽象的小说列表抓取，已经实现了解析tr元素的方法
 */
public abstract class AbstractNovelSpider extends AbstractSpider implements INovelSpider {
    private static final Logger logger = LogManager.getLogger(AbstractNovelSpider.class.getName());


    protected Element nextPageElement;
	/** 下一页的url */
	protected String nextPage;
	/**
	 * 默认的抓取方法，最多会尝试 {@link INovelSpider#MAX_TRY_TIMES} 次下载
	 * @param url
	 * @return
	 * @throws Exception
	 */
	protected Elements getsTr(String url) throws Exception {
		return getsTr(url, INovelSpider.MAX_TRY_TIMES);
	}
	/**
	 * 以指定次数的形式去解析对应网页
	 * @param url
	 * @param maxTryTimes 如果为null， 则 默认是 {@link INovelSpider#MAX_TRY_TIMES}
	 * @return
	 * @throws Exception
	 */
	protected Elements getsTr(String url, Integer maxTryTimes) throws Exception {
        logger.info("开始解析:"+url);
		if(url.equals("http://www.5858xs.com/xiaoshuotoplastupdate/0/3/3393.html")){
			nextPage = "";
			return null;
		}
		maxTryTimes = maxTryTimes == null ? INovelSpider.MAX_TRY_TIMES : maxTryTimes;
		Elements trs = null;
		for (int i = 0; i < maxTryTimes ; i++) {
			try {
				String result = super.crawl(url);
				Map<String, String> context = NovelSpiderUtil.getContext(NovelSiteEnum.getEnumByUrl(url));
				String novelSelector = context.get("novel-selector");
				if (novelSelector == null) throw new RuntimeException(NovelSiteEnum.getEnumByUrl(url).getUrl() + ",url=" + url + "目前不支持抓取小说列表");
				Document doc = Jsoup.parse(result);
				doc.setBaseUri(url);
				trs = doc.select(novelSelector);
				
				String nextPageSelector = context.get("novel-nextpage-selector");
				Elements nextPageElements = null;
				if (nextPageSelector != null) {

					if (nextPageSelector.contains(",")){
						String[] nexts = nextPageSelector.split(",");
						if (url.equals("http://www.5858xs.com/xiaoshuotoplastupdate/0/0/1.html")){
							nexts[1] = "12";
						}

						nextPageElement = doc.select(nexts[0]).get(Integer.parseInt(nexts[1]));
					}else {
						nextPageElements = doc.select(nextPageSelector);
						nextPageElement = nextPageElements == null ? null : nextPageElements.first();
					}

					
					if (nextPageElement != null) {
						nextPage = nextPageElement.absUrl("href");
						
					} else {
						nextPage = "";
					}
				}
				return trs;
			} catch (Exception e) {
				
			}
		}
		throw new RuntimeException(url + ",尝试了" + maxTryTimes + "次依然下载失败了！");
	}
	
	@Override
	public boolean hasNext() {
		return !nextPage.isEmpty();
	}

	@Override
	public String next() {
		return nextPage;
	}
	@Override
	public Iterator<List<Novel>> iterator(String firstPage, Integer maxTryTimes) {
		nextPage = firstPage;
		return new NovelIterator(maxTryTimes);
	}
	/**
	 * 一个迭代器，专门用于对小说网站数据列表抓取
	 */
	private class NovelIterator implements Iterator<List<Novel>> {
		private Integer maxTryTimes;
		public NovelIterator(Integer maxTryTimes) {
			this.maxTryTimes = maxTryTimes;
		}
		@Override
		public boolean hasNext() {
			return AbstractNovelSpider.this.hasNext();
		}
		@Override
		public List<Novel> next() {
            List<Novel> novels = null;
            try {
                novels = getsNovel(nextPage, maxTryTimes);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return novels;
		}
	}
}
