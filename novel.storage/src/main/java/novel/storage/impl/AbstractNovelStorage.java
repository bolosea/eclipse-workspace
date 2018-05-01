package novel.storage.impl;

import novel.spider.entitys.Novel;
import novel.spider.interfaces.INovelSpider;
import novel.spider.util.NovelSpiderFactory;
import novel.storage.Processor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AbstractNovelStorage implements Processor {
	protected SqlSessionFactory sqlSessionFactory;
	protected Map<String, String> tasks = new TreeMap<>();
	public AbstractNovelStorage() throws FileNotFoundException {
		sqlSessionFactory = new SqlSessionFactoryBuilder().build(new FileInputStream("conf/SqlMapConfig.xml"));
	}


	@Override
	public void process(String action) {

	}

	/**
     * 根据action处理任务
     */
	public void process(String action,int maxTry) {
		ExecutorService service = Executors.newFixedThreadPool(tasks.size());
		List<Future<String>> futures = new ArrayList<>(tasks.size());
		for (Entry<String, String> entry : tasks.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();
			futures.add(service.submit(new Callable<String> () {
				@Override
				public String call() throws Exception {
					INovelSpider spider = NovelSpiderFactory.getNovelSpider(value);
					Iterator<List<Novel>> iterator = spider.iterator(value, 10);
					while (iterator.hasNext()) {
						System.err.println("开始"+action+"[" + key + "] 的 URL:" + spider.next());
						int i=0;
						try {
							for (;i<maxTry;i++){
								List<Novel> novels = iterator.next();
								SqlSession session = sqlSessionFactory.openSession();
								if (action.equalsIgnoreCase("batchInsert")){
									for (Novel novel : novels) {
										novel.setFirstLetter(key.charAt(0));//设置小说的名字的首字母
									}
									session.insert(action, novels);
								}else if(action.equalsIgnoreCase("batchUpdate")){
									session.update(action,novels);
								}
								session.commit();
								session.close();
								Thread.sleep(1_000);
							}
						}catch (Exception e){
							e.printStackTrace();
							System.out.println(key+"尝试了"+i+"/"+maxTry+"次都失败了~");
						}

					}
					return key;
				}
				
			}));
		}
		service.shutdown();
		for (Future<String> future : futures) {
			try {
				System.out.println(""+action+"[" + future.get() + "]结束！");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
