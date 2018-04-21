package novel.spider.impl.novel;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import novel.spider.NovelSiteEnum;
import novel.spider.entitys.Novel;
import novel.spider.util.NovelSpiderUtil;

/**
 * 笔下文学网站的书籍列表爬取
 */
public class BxwxNovelSpider extends AbstractNovelSpider {
	public BxwxNovelSpider() {}

	@Override
	public List<Novel> getsNovel(String url, Integer maxTryTimes) {
		List<Novel> novels = new ArrayList<>();
		try {
			Elements trs = super.getsTr(url, maxTryTimes);
			for (int index = 1, size = trs.size(); index < size; index++) {
				Element tr = trs.get(index);
				Elements tds = tr.getElementsByTag("td");
				Novel novel = new Novel();
				novel.setName(tds.first().text());
				String novelUrl = tds.first().getElementsByTag("a").first().absUrl("href").replace(".htm", "/").replace("/binfo/", "/b/");
				novel.setUrl(novelUrl);
				novel.setLastUpdateChapter(tds.get(1).text());
				novel.setLastUpdateChapterUrl(tds.get(1).getElementsByTag("a").first().absUrl("href"));
				novel.setAuthor(tds.get(2).text());
				novel.setLastUpdateTime(NovelSpiderUtil.getDate(tds.get(4).text(), "yy-MM-dd"));//2016-10-14 yyyy-MM-dd
				novel.setStatus(NovelSpiderUtil.getNovelStatus(tds.get(5).text()));
				novel.setPlatformId(NovelSiteEnum.getEnumByUrl(url).getId());
				novels.add(novel);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return novels;
	}
}
