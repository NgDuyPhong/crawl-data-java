package com.crawldata.root;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This is a tool crawls data!
 *
 */
public class App 
{
	// input key word search
	private static String keySearch = "倉庫　軽作業　千葉県流山市";
	// location is "" - (2km) or "10.0" - (10km)
	private static String location = "10.0";
	// Day posted options are "" - (all), "day", "3days", "week", "month"
	private static String dayPosted = "";
	// input bukken_id
	private static String bukken_id = "34";
	
	private static final String[] LIST_JOB_TYPES = {"フルタイム", "パートタイム", "契約社員", "インターン"};
	private final static String[] LIST_EXCLUSION_FLAG = {"フォーク","ﾌｫｰｸ"};
	private final static String AN_HOUR = "1 時間 ";
	private static final String[] HEADER_CSV = {
	            	// Title of the columns (column_names)
	            	"募集データID",
	            	"物件ID",
	            	"データ取得日",
	            	"距離条件",
	            	"除外フラグ",
	            	"募集主体",
	            	"提供元",
	            	"求人タイトル",
	            	"時給（from）",
	            	"時給（to）	雇用形態"
	};
	private static int countItem = 0;
	private static ArrayList<String> dataCsv = new ArrayList<String>();
	private static ArrayList<String> listHashRecruitmentID = new ArrayList<String>();

    public static void main( String[] args )
    {
    	System.out.println( "start crawler" );
		callApiCrawl();
    }
    
    public static int mainCrawlData(Document document, Date date) {
    	int count = 0;
    	String jobName = "";
    	String postPerson = "";
    	String salaryMin = "";
    	String salaryMax = "";
    	String jobType = "";
    	String provider = "";
    	String dateStr = "";
		String lradStr = location == "" ? "2km" : "10km";

    	Elements elms = document != null ? document.getElementsByClass("PwjeAc") : null;
    	countItem = count = elms.size();

		if (elms != null)
		for (Element element : elms) {
			jobName = element.getElementsByClass("BjJfJf PUpOsf").text();
			postPerson = element.getElementsByClass("vNEEBe").text();
			provider = getProvider(element.getElementsByClass("Qk80Jf").text());
			dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);

			String exclusionFlag = "N";
			if (jobName.indexOf(LIST_EXCLUSION_FLAG[0]) != -1 || jobName.indexOf(LIST_EXCLUSION_FLAG[1]) != -1) {
				exclusionFlag = "Y";
			}

			jobType = "";
			salaryMin = "";
	    	salaryMax = "";
			Element el = element.getElementsByClass("oNwCmf").get(0);
			for (Element elChild : el.getElementsByClass("SuWscb")) {
				String elStr = elChild.text();
				if (Arrays.asList(LIST_JOB_TYPES).indexOf(elStr) != -1) {
					jobType = elChild.text();
				} else {
					if (elStr.indexOf(AN_HOUR) != -1) {
						String[] salary = elStr.replace(AN_HOUR, "").split("～");
						salaryMin = salary[0] != null ? salary[0] : "";
						if (salary.length == 1) {
							salaryMin = salary[0] != null ? formatSalary(salary[0]) : "";
						} else if (salary.length == 2) {
							salaryMin = salary[0] != null ? formatSalary(salary[0]) : "";
							salaryMax = salary[1] != null ? formatSalary(salary[1]) : "";
						}
					}
				}
			}
			String recruitmentID = "1" + provider + "2" + jobName + "3" + salaryMin + "4" + salaryMax + "5" + jobType;
			String hashRecruitmentID = hashMd5(recruitmentID);

			if (listHashRecruitmentID.indexOf(hashRecruitmentID) == -1) {
				listHashRecruitmentID.add(hashRecruitmentID);
				String data = hashRecruitmentID + "," + bukken_id + ","
						+ dateStr + "," + lradStr + "," + exclusionFlag + "," + postPerson + "," + provider + ","
						+ jobName + "," + salaryMin + "," + salaryMax + "," + jobType;

				dataCsv.add(data);
//				System.out.println(exclusionFlag + " -- " + postPerson + " -- " + provider + " -- " + jobName + " -- " + salaryMin + " -- " + salaryMax + " -- " + jobType);
			} else {
				// sum item of file output-- if recruitmentID exists
				if (countItem > 0) {
					countItem--;
				} else {
					countItem = 0;
				}
			}
//				hashRecruitmentID, bukken_id, date, lradStr, exclusionFlag, postPerson, provider, jobName, salaryMin, salaryMax, jobType
		}

		return count;
    }

    public static void callApiCrawl() {
    	
    	int sumInWhile = 0;
		int start = 0;
		int count = 10;
		int sum = 0;
		Date date = new Date();

		try {
			while(sumInWhile < 150 && count > 0) {
	    		count = 10;
    			String lrad = "&lrad=10.0"; // req.location != "2" ? "&lrad=10.0" : "";
    			String chips = dayPosted != "" ? ("&chips=date_posted:" + dayPosted + "&schips=date_posted;" + dayPosted) : "";
    			String q = URLEncoder.encode(keySearch, StandardCharsets.UTF_8.toString());
    			String urls = "https://www.google.com/search?yv=3&rciv=jb&"
    					+ lrad + chips + "nfpr=0&"
    					+ "q=" + q + "&start=" + start
    					+ "&asearch=jb_list&async=_id:VoQFxe,_pms:hts,_fmt:pc";
    			String userAgent = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
    			
    			// call api
    			Document document = Jsoup.connect(urls)
    					.header("Accept-Language", "ja;q=0.9,fr;q=0.8,de;q=0.7,es;q=0.6,it;q=0.5,nl;q=0.4,sv;q=0.3,nb;q=0.2")
    					.userAgent(userAgent)
    					.get();

    			// get value crawl
    			count = mainCrawlData(document, date);
 
    			if (countItem != 0) {
    				sum += countItem;
    				System.out.println("Total number of rows crawled: " + (sum));
    			} else {
    				System.out.println(sum + "," + count);
    			}
    			sumInWhile += count;
    			start += 10;
			}

			// write csv
			writeCsv(date, sum);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Error call api");
			e.printStackTrace();
		}
    }
    
    public static void writeCsv(Date date, int sum) {
    	try {
    		String fileType = ".csv";
    		String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(date);
    		
    		String fileName = keySearch + "_" + dateStr + "_" + sum + "_" + fileType;
    		
    		PrintWriter writer = new PrintWriter(new File("data/" + fileName));
            StringBuilder sb = new StringBuilder();

            // format font utf-8 when open file with excel
            sb.append("\uFEFF");

            // write header
            for (int i = 0; i < HEADER_CSV.length; i++) {
				if (i == HEADER_CSV.length - 1) {
					sb.append(HEADER_CSV[i]);
				} else {
					sb.append(HEADER_CSV[i] + ",");
				}
			}
            sb.append('\n');

            // write content
            for (int i = 0; i < dataCsv.size(); i++) {
            	sb.append(dataCsv.get(i));
            	sb.append('\n');
			}
            writer.write(sb.toString());
            writer.flush();
            writer.close();

            System.out.println("write csv done! " + fileName);

          } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
          }
    }
    
    public static String hashMd5(String hashStr) {
    	String hashtext = "";
    	try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(hashStr.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			hashtext = bigInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return hashtext;
    }

    public static String formatSalary(String salary) {
    	if (salary != null) {
    		salary = salary.replace(AN_HOUR, "");
    		salary = salary.replace("￥", "");
    		salary = salary.replace(",", "");
    	}
    	return salary;
    }

    public static String getProvider(String provider) {
    	if (provider != null) {
    		if (provider.split(": ").length >= 2) {
    			provider = provider.split(": ")[1];
    		}
    	}
    	return provider;
    }
}
