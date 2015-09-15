package html2pdf5;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.tidy.Tidy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Component
public class ScheduledTasks {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	Tidy tidy = getTidy();
	DOMReader domReader = new DOMReader();
	String domain = "http://workshop-manuals.com";
	String ford1 ="http://workshop-manuals.com/ford/c-max_2003.75_06.2003/mechanical_repairs/1_general_information/100_service_information/100-00_general_information/description_and_operation/about_this_manual/";
	int yearMin =  1996;
	int yearMax =  1996;
	//develop
//	String basicDir ="/home/roman/jura/";
	String oUT1json = "/OUT1json";
	//prodaction windows
//	String basicDir ="d:\\home\\roman\\jura\\";
//	String oUT1json = "\\OUT1json";
	//prodaction
	String basicDir ="/home/holweb/jura/";
	String outputDir = basicDir + "workshop-manuals"
			+ yearMin
			+ "-"
			+ yearMax
			+ oUT1json;
//	String outputDir ="/home/holweb/jura/workshop-manuals.com/OUT1";
	
	private Document autoIndexDocument;
	private Element bodyElAutoIndexDocument;
	private int cnt1 = 0, cnt2 = 0, cntVehicles = 0;
	private	int fileIdx = 0;
	DateTime startMillis;
	
	@Scheduled(fixedRate = 500000000)
	public void reportCurrentTime() {
		startMillis = new DateTime();
		System.out.println("The time is now " + dateFormat.format(startMillis.toDate()));
		domReader.setDocumentFactory(new DOMDocumentFactory());
		
//		readAuto(ford1, "c-max_2003.75_06.2003", "ford");
//		if(true) return;
		
//		createAutoIndexDocument();
		
		Document document = getDomFromStream(domain);
		logger.debug(outputDir);
		File openOutputFolder = openCreateFolder(outputDir);
		
		logger.debug(""+openOutputFolder);
		List<Element> selectNodes = document.selectNodes("/html/body/div/table//a[contains(@href,'workshop-manuals')]");
		for (Element manufacturerAncorElement : selectNodes) {
//			addOl1li(element);
			String href = manufacturerAncorElement.attributeValue("href");
			if(href.equals(domain)) continue;
			String[] split = href.split("/");
			String manufacturer = split[split.length - 1];
			logger.debug(href+" :: "+manufacturer);
//			if(new File(outputDir+"/"+manufacturer).exists())
//				continue;
			File dm = openCreateFolder(outputDir+"/"+manufacturer);
			Document document2 = getDomFromStream(href);
			if(document2 != null)
			{
				readManufacturer(manufacturerAncorElement, href, manufacturer, document2);
			}
			cnt1++;
//			if(cnt1 >=1)
//				break;
		}
		String msg = "url "+domain + " :: overall "+selectNodes.size()+"/"+cntVehicles;
//		bodyElAutoIndexDocument.addText(msg);
		//logger.debug(msg);
//		saveAutoIndexDocument();
	}
	private void readManufacturer(Element manufacturerAncorElement, String href, String manufacturer, Document document2) {
		List<Element> allManufacturerAutos = document2.selectNodes("/html/body/div/table//a[contains(@href,'"
				+ manufacturer
				+ "')]");
//				addOl1ToLastLi(selectNodes2);
		logger.debug(href+" :: "+allManufacturerAutos.size());
		if(true){
			cnt2=0;
			for (Element autoElement : allManufacturerAutos) {
				//						addOl1Ol2Li(element2);
				String autoHref = autoElement.attributeValue("href");
				
				DOMElement parent = (DOMElement) autoElement.getParent();
				DOMElement previousSibling = (DOMElement) parent.getPreviousSibling();
				String autoName = autoElement.getText().trim();
				if(previousSibling != null)
				{
					if(previousSibling.getName().equals("h2"))
					{
						autoName = "-- "+ previousSibling.getText()+" -- "+ autoName;
					}
				}
				autoName = autoName.trim();
				String replace = autoHref.replace("/", "");
				if(replace.equals(manufacturer) 
						|| autoHref.equals(domain)
						) continue;
				//				if(!autoName.contains(yearMin))					continue;
				for (int y = yearMin; y <= yearMax; y++) {
					if(autoName.contains(""+y)){
//						autoDocument = createAutoDocument();

						autoTileAllIndexNr = 0;
						String htmlOutFileName = getHtmlOutFileName(autoName.replaceAll(" ", "_"), manufacturer);
						String htmlOutFileName2 = htmlOutFileName+".json";
						logger.debug(htmlOutFileName2);
						File f = new File(htmlOutFileName2);
						if(f.exists())
						{
							logger.debug("f.exists() --  "+htmlOutFileName2);
							continue;
						}

						readAutoIndexList(autoHref, autoName, manufacturer);
						//				readAuto(autoHref, autoName, manufacturer);
						cnt2++;
						System.out.println("-----------------------------------------------------"+cnt2);
						//				if(cnt2==1)
						//					break;
					}
				}
			}
		}
	}
	private void initAutoData() {
		autoData = new HashMap<String, Object>();
		autoData.put("workPath", new ArrayList<Integer>());
		initIndexList(autoData);
	}
	private List<Map<String, Object>> initIndexList(Map<String, Object> autoData) {
		if(!autoData.containsKey("indexList")){
			autoData.put("indexList", new ArrayList<Map<String, Object>>());
		}
		return (List<Map<String, Object>>) autoData.get("indexList");
	}
//	Document autoDocument;
	Map<String, Object> autoData;
	
	int autoTileAllIndexNr = 0;

	private void readAutoIndexList(String autoTileHref, String autoName, String manufacturer) {
		autoTileAllIndexNr = 0;
		initAutoData();
		readNextAutoIndexList(autoTileHref);
		while(nextAutoTileElement != null){
			getNextTrueSibling();
			if(nextAutoTileElement == null){
				break;
			}
			Attribute hrefAttribute = (Attribute) nextAutoTileElement.selectSingleNode("a/@href");
			String hrefAutoTileNext = hrefAttribute.getValue();
			readNextAutoIndexList(hrefAutoTileNext);
		}
		try{
//			buildBookmark(autoDocument);
			autoName = autoName.replaceAll(" ", "_");
			String htmlOutFileName = getHtmlOutFileName(autoName, manufacturer);
//			saveHtml(autoDocument, htmlOutFileName);
//			savePdf(htmlOutFileName, htmlOutFileName+".pdf");
			writeToJsonDbFile(autoData, htmlOutFileName+".json");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private String getHtmlOutFileName(String autoName, String manufacturer) {
		autoName = autoName.replace("/", ":");
		String htmlOutFileName = outputDir+"/"+manufacturer+"/"+manufacturer+"_"+autoName+".html";
		return htmlOutFileName;
	}

	private void readNextAutoIndexList(String autoTileHref) {
		DOMDocument autoTileContextDom = null;
		try{
			Document autoTileDom = getDomFromStream(autoTileHref);
			String indexHrefAdd = ((Attribute) autoTileDom.selectSingleNode("/html/body//iframe/@src")).getValue();
			String hrefContent = autoTileHref+""+indexHrefAdd;
			autoTileContextDom = getDomFromStream(hrefContent);
		}catch (Exception e){
			List<Map<String, Object>> workIndexList = initIndexList(autoData);
			Map<String, Object> workContentItem = workIndexList.get(workIndexList.size() - 1);
			HashMap<String, Object> contextItem = new HashMap<String, Object>();
			contextItem.put("text", "BAD PAGE");
			contextItem.put("error", e.getMessage());
			contextItem.put("url", autoTileHref);
			workIndexList.add(contextItem);
			logger.error(e.getMessage());
			nextAutoTileElement = null;
			return ;
		}
		if (autoTileContextDom == null)
			return ;
		List<DOMElement> myPagePosition = (List<DOMElement>) autoTileContextDom.selectNodes("/html/body/div/p[@style and not(a)]");
		if(myPagePosition.size() == 0)
		{//bad link auto tile page not exist
			logger.debug(nextAutoTileElement.asXML());
			nextAutoTileElement = (DOMElement) nextAutoTileElement.getNextSibling();
			logger.debug(nextAutoTileElement.asXML());
			return ;
		}
		Map<String, Object> contextItem = null;
		for (Element element : myPagePosition) {
			String text = element.getText();
			String style = element.attribute("style").getValue();
			String level = style.split(";")[0].split("padding-left:")[1].split("pt")[0];
			int levelInt = Integer.parseInt(level)/10;
			if(levelInt < 0 && !autoData.containsKey("autoName")){
				autoData.put("autoName", text);
			}else if(levelInt >= 0){
				int indexOfcurrent = text.indexOf(">>");
				if(indexOfcurrent == 0)
				{
					levelInt = levelInt + 1;
					text = text.replace(">>", "").replace("<<", "").trim();
					logger.debug(autoTileAllIndexNr+"/"+levelInt+" -- "+text);
//					logger.debug(autoTileIndexNr+"/"+autoTileAllIndexNr+"/"+levelInt+" -- "+text);
				}
				contextItem = new HashMap<String, Object>();
				contextItem.put("text", text);
				Map<String, Object> workContentItem = autoData;
				List<Map<String, Object>> workIndexList = initIndexList(workContentItem);
				for (int i = 0; i < levelInt; i++) {
					if(workIndexList.size() == 0)//not skip level
						break;
					workContentItem = workIndexList.get(workIndexList.size() - 1);
					workIndexList = initIndexList(workContentItem);
				}
				workIndexList.add(contextItem);
			}
		}
		
		DOMElement lastElement = myPagePosition.get(myPagePosition.size() - 1);
		contextItem.put("url", autoTileHref);
		nextAutoTileElement = (DOMElement) lastElement.getNextSibling();
//		nextAutoTileElement = getLastIndexElement(nextAutoTileElement);
		
		autoTileAllIndexNr++;
		
	}
	//for test
	private DOMElement getLastIndexElement(DOMElement lastElement) {
		nextAutoTileElement = (DOMElement) lastElement.getNextSibling();
		if(nextAutoTileElement == null)
		{
			return lastElement;
		}
		logger.debug(nextAutoTileElement.selectSingleNode("a/text()").asXML());
		return getLastIndexElement(nextAutoTileElement);
	}
	private void getNextTrueSibling() {
			Node selectSingleNode = nextAutoTileElement.selectSingleNode("a/@href");
			String stringValue = selectSingleNode.getStringValue();
			if(stringValue.equals(domain))
				nextAutoTileElement = (DOMElement) nextAutoTileElement.getNextSibling();
	}
	DOMElement nextAutoTileElement = null;

	void writeToJsonDbFile(Object java2jsonObject, String fileName) {
		File file = new File(fileName);
		logger.warn(""+file);
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();
		try {
			//			logger.warn(writerWithDefaultPrettyPrinter.writeValueAsString(java2jsonObject));
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			writerWithDefaultPrettyPrinter.writeValue(fileOutputStream, java2jsonObject);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Set<String> contentSet ;
	String autoTileName ;
	int h2Count = 0;

	String procentWorkTime() {
		int procent = 0;
//		int procent = fileIdx*100/filesCount;
		String workTime = hmsFormatter.print(new Period(startMillis, new DateTime()));
		String procentSecond = " - html2pdf3 - (" + procent + "%, " + workTime + "s)";
		return procentSecond;
	}
	static PeriodFormatter hmsFormatter = new PeriodFormatterBuilder()
			.appendHours().appendSuffix("h ")
			.appendMinutes().appendSuffix("m ")
			.appendSeconds().appendSuffix("s ")
			.toFormatter();
	private void buildBookmark(Document autoDocument) {
		List<Element> bookmarkEls = autoDocument.selectNodes("/html/body//*[@data-bookmark='h2' or @data-bookmark='h3' or @data-bookmark='h4']");
		logger.debug(""+bookmarkEls.size());
		Element headEl = (Element) autoDocument.selectSingleNode("/html/head");
		Element bookmarks = headEl.addElement("bookmarks")
				,h2 = null, h3 = null, h4 = null;
		int h2i = 0, h3i = 0, h4i = 0;
		String h2id = null, h3id = null, h4id = null;
		int i = 0;
		for (Element h234Element : bookmarkEls) {
			i++;
			if(h234Element.attribute("data-bookmark").getValue().equals("h2"))
			{
				h2 = bookmarks.addElement("bookmark");
				h2id = "bm"+h2i++;
				makeBookmark(h2, h234Element, h2id);
			}
			else
				if(h234Element.attribute("data-bookmark").getValue().equals("h3"))
				{
					h3 = h2.addElement("bookmark");
					h3id = h2id + "."+h3i++;
					makeBookmark(h3, h234Element, h3id);
				}
				else
					if(h234Element.attribute("data-bookmark").getValue().equals("h4"))
					{
						h4 = h3.addElement("bookmark");
						h4id = h3id + "."+h4i++;
						makeBookmark(h4, h234Element, h4id);
					}
			
		}
	}

	private void makeBookmark(Element h234InHead, Element h234Element, String id) {
		Element h234A_Element = (Element) h234Element.selectSingleNode("a");
		String text = h234A_Element.getText();
		h234InHead.addAttribute("name", text);
		h234InHead.addAttribute("href", "#"+id);
		h234A_Element.addAttribute("name", id);
	}

	private void changeImgUrl(Element autoTileElement) {
		List<Element> selectNodes = autoTileElement.selectNodes(".//img");
		for (Element bagroundImage : selectNodes) {
			Attribute srcImg = bagroundImage.attribute("src");
			if(!srcImg.getValue().contains(domain))
			srcImg.setValue(domain+"/"+srcImg.getValue());
		}
//		Element bagroundImage = (Element) autoTileElement.selectSingleNode("img[1]");
	}

	private void saveHtml(Document document, String htmlOutFileName) {
		Element headEl = (Element) document.selectSingleNode("/html/head");
		addUtf8(headEl);
		writeToFile(document, htmlOutFileName);
	}
	
	private List<Element> getAutoTileContextIndex(String autoHref, Document domFromStream) {
		String indexHrefAdd = ((Attribute) domFromStream.selectSingleNode("/html/body//iframe/@src")).getValue();
		String hrefIndex = autoHref+""+indexHrefAdd;
		logger.debug("\n hrefIndex = "+hrefIndex);
		return (List<Element>) getDomFromStream(hrefIndex).selectNodes("/html/body/div//a");
	}

	private Document createAutoDocument() {
		Document autoDocument = DocumentHelper.createDocument();
		Element htmElAutoDocument = autoDocument.addElement("html");
		Element headElAddElement = htmElAutoDocument.addElement("head");
		addUtf8(headElAddElement);
		htmElAutoDocument.addElement("body");
		return autoDocument;
	}

	private void readManualIndex(String msg) {
		logger.debug(msg);
		//logger.debug(""+domFromStream);
		List<Element> selectNodes2 = getDomFromStream(msg).selectNodes("/html/body/div//a");
		for (Element autoTileElement : selectNodes2) {
			String autoTileHref = autoTileElement.attributeValue("href");
			String text = autoTileElement.getText();
			System.out.println(autoTileHref);
			System.out.println(text);
			if(autoTileHref.indexOf("http")<0
					|| autoTileHref.equals("http://workshop-manuals.com"))
				continue;
			//logger.debug(attributeValue3);
		}
	}

	private File openCreateFolder(String dir) {
		File file = new File(dir);
		if(!file.exists())
		{
			file.mkdirs();
		}
		return file;
	}

	//-------------index file--------------------------
	private void addOl1Ol2Li(Element element2) {
		Element ol21El = (Element) bodyElAutoIndexDocument.selectObject("ol/li[last()]/ol");
		Element li2El = ol21El.addElement("li");
		li2El.add((Element) element2.clone());
	}

	private void addOl1ToLastLi(List<Element> selectNodes2) {
		Element liEl = (Element) bodyElAutoIndexDocument.selectObject("ol/li[last()]");
		liEl.addText(" :: "+selectNodes2.size());
		cntVehicles += selectNodes2.size();
		liEl.addElement("ol");
	}

	private Element addOl1li(Element element) {
		Element olEl = (Element) bodyElAutoIndexDocument.selectObject("ol");
		Element liEl = olEl.addElement("li");
		liEl.add((Element) element.clone());
		return liEl;
	}
	private void createAutoIndexDocument() {
		autoIndexDocument = DocumentHelper.createDocument();
		Element htmElAutoDocument = autoIndexDocument.addElement("html");
		Element headElAddElement = htmElAutoDocument.addElement("head");
		addUtf8(headElAddElement);
		bodyElAutoIndexDocument = htmElAutoDocument.addElement("body");
		Element olEl = bodyElAutoIndexDocument.addElement("ol");
	}

	private void saveAutoIndexDocument() {
		writeToFile(autoIndexDocument, "autoIndex.html");
	}
	//-------------index file--------------------------END

	private DOMDocument getDomFromStream(String url) {
		HttpURLConnection urlConnection = getUrlConnection(url);
		try {
			return getDomFromStream(urlConnection);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private DOMDocument getDomFromStream(HttpURLConnection urlConnection) throws IOException {
		InputStream requestBody = urlConnection.getInputStream();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(requestBody));
		String line = null;
		StringBuilder responseData = new StringBuilder();
		while((line = in.readLine()) != null) {
			if(line.contains("font size>")){
				line = line.replace("font size>", "font>");
			}
			if(line.contains("<g:plusone size=\"small\" annotation=\"none\"></g:plusone>")){
				line = line.replace("<g:plusone size=\"small\" annotation=\"none\"></g:plusone>", "");
			}
			responseData.append(line);
		}
		InputStream byteArrayInputStream = new ByteArrayInputStream(responseData.toString().getBytes(StandardCharsets.UTF_8));
		
		org.w3c.dom.Document html2xhtml = tidy.parseDOM(byteArrayInputStream, null);
//		org.w3c.dom.Document html2xhtml = tidy.parseDOM(requestBody, null);
		DOMDocument document = (DOMDocument) domReader.read(html2xhtml);
		return document;
	}

	private HttpURLConnection getUrlConnection(String url) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "text/html"); 
			con.setRequestProperty("charset", "utf-8");
			return con;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Tidy getTidy() {
		Tidy tidy = new Tidy();
		tidy.setShowWarnings(false);
		tidy.setXmlTags(false);
		tidy.setInputEncoding("UTF-8");
		tidy.setOutputEncoding("UTF-8");
		tidy.setXHTML(true);// 
		tidy.setMakeClean(true);
		tidy.setQuoteNbsp(false);
		return tidy;
	}
	
	OutputFormat prettyPrintFormat = OutputFormat.createPrettyPrint();
	private void writeToFile(Document document, String htmlOutFileName) {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(htmlOutFileName);
			//					HTMLWriter xmlWriter = new HTMLWriter(fileOutputStream, prettyPrintFormat);
			XMLWriter xmlWriter = new XMLWriter(fileOutputStream, prettyPrintFormat);
			xmlWriter.write(document);
			xmlWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void addUtf8(Element headEl) {
		headEl.addElement("meta").addAttribute("charset", "utf-8");
	}
	
	
	public void manualsCo() {
		String domain = "http://manuals.co";
		System.out.println("The time is now " + dateFormat.format(new Date()));
		Document document = getDomFromStream(domain + "/workshop/");
		//logger.debug(""+document);
		List<Element> selectNodes = document.selectNodes("/html/body//ul[@class='brands_alpha']/li/a");
		int cntVehicles = 0;
		for (Element element : selectNodes) {
			String href = element.attributeValue("href");
			Document document2 = getDomFromStream(domain + href);
			if(document2 != null)
			{
				List<Element> selectNodes2 = document2.selectNodes("/html/body//ul[@class='brands_alpha list-models']/li/a");
				cntVehicles += selectNodes2.size();
				//logger.debug(domain+href+" :: "+selectNodes2.size());
				if(false)
					for (Element element2 : selectNodes2) {
						String href2 = element2.attributeValue("href");
						//logger.debug(domain+href2);
					}
			}
			//			break;
		}
		//logger.debug("url "+domain + "/workshop/ :: overall "+selectNodes.size()+"/"+cntVehicles);
	}
	
}
