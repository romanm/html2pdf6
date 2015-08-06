package html2pdf5;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.util.HashSet;
import java.util.Iterator;
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
import org.xhtmlrenderer.pdf.ITextRenderer;

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
	//develop
	String outputDir ="/home/roman/algoritmed.com/jura-boris/workshop-manuals.com/OUT";
	//prodaction
//	String outputDir ="/home/holweb/jura/workshop-manuals.com";
	
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
			if(new File(outputDir+"/"+manufacturer).exists())
				continue;
			File dm = openCreateFolder(outputDir+"/"+manufacturer);
			Document document2 = getDomFromStream(href);
			if(document2 != null)
			{
				readManufacturer(manufacturerAncorElement, href, manufacturer, document2);
			}
			cnt1++;
			if(cnt1 >=1)
				break;
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
				String autoName = autoElement.getText().trim();
				String replace = autoHref.replace("/", "");
				if(replace.equals(manufacturer) 
						|| autoHref.equals(domain)
						) continue;
				autoDocument = createAutoDocument();
				
				autoTileAllIndexNr = 0;
				readAutoIndexList(autoHref, autoName, manufacturer);
//				readAuto(autoHref, autoName, manufacturer);
				cnt2++;
				System.out.println("-----------------------------------------------------"+cnt2);
				if(cnt2==1)
					break;
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
	Document autoDocument;
	Map<String, Object> autoData;
	
	int autoTileAllIndexNr = 0;
	int autoTileIndexNr = 0;
	private void readAutoIndexList(String autoTileHref, String autoName, String manufacturer) {
		autoTileIndexNr = 0;
		initAutoData();
		readNextAutoIndexList(autoTileHref);
		while(nextAutoTileElement != null){
			autoTileIndexNr = 0;
			Attribute hrefAttribute = (Attribute) nextAutoTileElement.selectSingleNode("a/@href");
			String hrefAutoTileNext = hrefAttribute.getValue();
			boolean readNextAutoIndexList = readNextAutoIndexList(hrefAutoTileNext);
			if(!readNextAutoIndexList)
				break;
		}
		try{
			buildBookmark(autoDocument);
			autoName = autoName.replaceAll(" ", "_");
			String htmlOutFileName = outputDir+"/"+manufacturer+"/"+manufacturer+"_"+autoName+".html";
			saveHtml(autoDocument, htmlOutFileName);
			savePdf(htmlOutFileName, htmlOutFileName+".pdf");
			writeToJsonDbFile(autoData, htmlOutFileName+".json");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private boolean readNextAutoIndexList(String autoTileHref) {
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
			return false;
		}
		if (autoTileContextDom == null)
			return false;
		List<DOMElement> myPagePosition = (List<DOMElement>) autoTileContextDom.selectNodes("/html/body/div/p[@style and not(a)]");
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
					logger.debug(autoTileIndexNr+"/"+autoTileAllIndexNr+"/"+levelInt+" -- "+text);
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
		contextItem.put("url", autoTileHref);
		DOMElement lastElement = myPagePosition.get(myPagePosition.size() - 1);
		nextAutoTileElement = (DOMElement) lastElement.getNextSibling();
		nextAutoTileElement = getLastIndexElement(nextAutoTileElement);
		
		if(nextAutoTileElement != null){
			autoTileIndexNr++;
			autoTileAllIndexNr++;
			if(autoTileIndexNr > 1000)
				return true;
			Attribute hrefAttribute = (Attribute) nextAutoTileElement.selectSingleNode("a/@href");
			String hrefAutoTileNext = hrefAttribute.getValue();
			readNextAutoIndexList(hrefAutoTileNext);
		}
		return false;
	}
	//for test
	private DOMElement getLastIndexElement(DOMElement lastElement) {
		nextAutoTileElement = (DOMElement) lastElement.getNextSibling();
		if(nextAutoTileElement == null)
		{
			return lastElement;
		}
		return getLastIndexElement(nextAutoTileElement);
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
	private void readAuto(String autoTileHref, String autoName, String manufacturer) {
		h2Count = 1;
		Document autoDocument = createAutoDocument();
		Element autoDocBody = (Element) autoDocument.selectSingleNode("/html/body");
		autoDocBody.addElement("h1").addText( manufacturer.toUpperCase()+ " :: "+ autoName);
		
		contentSet = new HashSet<String>();
		
		addAutoTileH2(autoTileHref, manufacturer, autoDocBody);
		
		try{
			buildBookmark(autoDocument);
			String htmlOutFileName = outputDir+"/"+manufacturer+"/"+manufacturer+"_"+autoName+".html";
			saveHtml(autoDocument, htmlOutFileName);
			savePdf(htmlOutFileName, htmlOutFileName+".pdf");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void addAutoTileH2(String autoTileHref, String manufacturer, Element autoDocBody) {
		logger.debug(autoTileHref);
		contentSet.add(autoTileHref);
		Document domFromStream = getDomFromStream(autoTileHref);
		Element titleElement = (Element) domFromStream.selectSingleNode("/html/head/title");
		String[] split = titleElement.getText().split(">");
		autoTileName = split[1].trim();// from title
		String h2level = split[2].trim();
		logger.debug("h2level "+h2level);
		autoDocBody.addElement("h2").addAttribute("data-bookmark", "h2")
		.addElement("a").addAttribute("name", "h2_"+h2Count).addText(h2level);
		String indexHrefAdd = ((Attribute) domFromStream.selectSingleNode("/html/body//iframe/@src")).getValue();
		String hrefContent = autoTileHref+""+indexHrefAdd;
		DOMDocument contextDocument = getDomFromStream(hrefContent);
		String nextH2Href = null;
		List<DOMElement> addH3 = addH3(autoDocBody, h2level, h2Count, contextDocument);
		DOMElement element1 = addH3.get(addH3.size()-1);
		while (element1.getNextSibling() != null) {
			element1 = (DOMElement) element1.getNextSibling();
			DOMElement element = (DOMElement) element1.getFirstChild();
			autoTileHref = element.attribute("href").getValue();
			autoTileName = element.getText();
			if(autoTileHref.contains("privacy.php")
					|| autoTileHref.replaceAll("/", "").equals(manufacturer)
					|| autoTileHref.equals(domain)
					) continue;
			if(contentSet.add(autoTileName)){
				logger.debug(autoTileName+"/"+h2level+"/"+(autoTileName.contains(h2level)));
				if(autoTileName.contains(h2level)){
				}else{
					logger.debug("------add new addAutoTileH2--------------");
					h2Count++;
					nextH2Href = autoTileHref;
					break;
				}
			}
		}
		addAutoTile();

		/*
		List<DOMElement> autoTileContextIndex = (List<DOMElement>) contextDocument.selectNodes("/html/body/div/p/a");
		DOMElement firstContextLink = autoTileContextIndex.get(0);
		for (Element element : autoTileContextIndex) {
		}
		 * */
		if(nextH2Href != null && h2Count < 5)
			addAutoTileH2(nextH2Href, manufacturer, autoDocBody);
	}

	private List<DOMElement> addH3(Element autoDocBody, String h2level,
			int autoTileNr, DOMDocument contextDocument) {
		List<DOMElement> myPagePosition = (List<DOMElement>) contextDocument.selectNodes("/html/body/div/p[not(a)]");
		for (Element element : myPagePosition) {
			String text = element.getText();
			if(
					text.equals("Donate and help keep workshop manuals freely available.")
					|| text.equals("Component Information")
					|| text.equals(">>Locations<<")
					|| text.equals(h2level)
					|| text.contains(autoTileName)
					)
				continue;
			text = text.replace(h2level, "").trim();//acura fall
			if(text.indexOf("-") == 0)
				text = text.substring(1).trim();
				
			if(contentSet.add(text))
			{
				autoDocBody.addElement("h3").addAttribute("data-bookmark", "h3")
				.addElement("a").addAttribute("name", "h3_"+autoTileNr).addText(text);
				break;
			}
		}
		return myPagePosition;
	}

	private void addAutoTile() {
		// TODO Auto-generated method stub
		
	}

	private void readAuto2(String autoTileHref, String autoName, String manufacturer) {
		logger.debug(autoTileHref);
		Document domFromStream = getDomFromStream(autoTileHref);

		Document autoDocument = createAutoDocument();
		Element autoDocBody = (Element) autoDocument.selectSingleNode("/html/body");
		autoDocBody.addElement("h1").addText(
				manufacturer.toUpperCase()+ " :: "+
				autoName);
		logger.debug(autoTileHref+" :: "+autoName);
		int autoTileNr = 1;
		addAutoTile(autoTileHref, autoTileNr, "t1", domFromStream, autoDocBody);

		List<Element> autoTileContextIndex = getAutoTileContextIndex(autoTileHref, domFromStream);
		logger.debug(
				"\n"+autoTileHref
				+"\n tiles count "+autoTileContextIndex.size());
		String autoTileH2 = "";
		String autoTileH3 = "";
		for (Element element : autoTileContextIndex) {
			autoTileHref = element.attribute("href").getValue();
			String autoTileName = element.getText();
			if(autoTileHref.contains("privacy.php")
					|| autoTileHref.replaceAll("/", "").equals(manufacturer)
					|| autoTileHref.equals(domain)
					)
				continue;
			autoTileNr++;
			String[] split = autoTileName.split(" - ");
			if(!autoTileH2.equals(split[0]))
			{
				autoDocBody.addElement("h2").addAttribute("data-bookmark", "h2").addElement("a").addAttribute("name", "h2_"+autoTileNr).addText(split[0]);
				autoTileH2 = split[0];
			}
			if(split.length > 1)
			{
//				autoDocBody.addElement("h3").addAttribute("data-bookmark", "h3").addElement("a").addAttribute("name", "h3_"+autoTileNr).addText(split[1]);
				domFromStream = getDomFromStream(autoTileHref);
				DOMElement autoTileElement = (DOMElement) domFromStream.selectSingleNode("/html/body//div[@id='page1-div']");
				if(autoTileElement != null){
					DOMDocument document = (DOMDocument) autoTileElement.getDocument();
					DOMElement h3El = (DOMElement) document.createElement("h3");
					h3El
					.addAttribute("data-bookmark", "h3")
					.addAttribute("style", "font-weight: bold; left: 0px; position: absolute; top: 3px; white-space: nowrap")
					.addElement("a").addAttribute("name", "h3_"+autoTileNr).addText(split[1]);
					DOMElement h4El = (DOMElement) autoTileElement.selectSingleNode("p");//.detach();
					autoTileElement.insertBefore(h3El, h4El);
					Node h4ElText = h4El.selectSingleNode("text()").detach();
					logger.debug(""+h4ElText.asXML()
					+"\n "+(h4El)
//					+"\n "+autoTileElement.asXML()
					);
					h4El.addAttribute("data-bookmark", "h4").addElement("a").addAttribute("name", "h4_"+autoTileNr).addText(h4ElText.getText());
					changeImgUrl(autoTileElement);
					
					Node detach = autoTileElement.detach();
					autoDocBody.add(detach);
				}else{
					autoDocBody.addElement("h3").addAttribute("data-bookmark", "h3").addElement("a").addAttribute("name", "h3_"+autoTileNr).addText(split[1]);
				}
			}
		}
		try{
			buildBookmark(autoDocument);
			String htmlOutFileName = outputDir+"/"+manufacturer+"/"+manufacturer+"_"+autoName+".html";
			saveHtml(autoDocument, htmlOutFileName);
			savePdf(htmlOutFileName, htmlOutFileName+".pdf");
		}catch(Exception e){
			e.printStackTrace();
		}
//		readManualIndex(msg);
	}

	void savePdf(String htmlOutFileName, String HTML_TO_PDF) throws com.lowagie.text.DocumentException, IOException {
		String url = new File(htmlOutFileName).toURI().toURL().toString();
		logger.debug(procentWorkTime()+" - start - "+HTML_TO_PDF);
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocument(url);
		renderer.layout();
		OutputStream os = new FileOutputStream(HTML_TO_PDF);
		renderer.createPDF(os);
		os.close();
		logger.debug(procentWorkTime()+" - end - "+HTML_TO_PDF);
	}

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

	private void makeBookmark2(Element h234InHead, Element h234Element, String id) {
		System.out.println(h234Element.asXML());
		System.out.println(1);
		String text = h234Element.getText();
		System.out.println(2);
		h234InHead.addAttribute("name", text);
		System.out.println(3);
		h234InHead.addAttribute("href", "#"+id);
		System.out.println(4);
		Node selectSingleNode = h234Element.selectSingleNode("text()");
		System.out.println(5);
		System.out.println(selectSingleNode);
		System.out.println(6);
		System.out.println(selectSingleNode.asXML());
		System.out.println(7);
		selectSingleNode.detach();
		System.out.println(8);
		h234Element.addElement("a").addAttribute("name", id).setText(text);
		System.out.println(9);
	}

	private String addAutoTile(String autoHref, int autoTileNr, String autoTileName, Document domFromStream, Element autoDocBody) {
		Element autoTileElement = (Element) domFromStream.selectSingleNode("/html/body//div[@id='page1-div']");
		if(autoTileElement != null){
			autoTileElement.attribute("id").setValue("auto_tile_"+autoTileNr);
			changeImgUrl(autoTileElement);
		}else{
			//audi
			Element autoTileElement2 = (Element) domFromStream.selectSingleNode(
					"/html/body/div/table//td[div/h2]");
			changeImgUrl(autoTileElement2);
			Element autoTileNameElement = (Element) autoTileElement2.selectSingleNode("div/h2");
			autoTileNameElement.setText(autoTileName);
			/*
			List<Element> breadcrum = autoTileElement2.selectNodes("div/h3");
			for (Element element : breadcrum) {
				element.detach();
			}
			 * */
			autoTileElement = autoDocBody.addElement("div");
			autoTileElement.addAttribute("id","auto_tile_"+autoTileNr);
			
			for (Iterator iterator = autoTileElement2.elementIterator(); iterator.hasNext();) {
				Element element = (Element) iterator.next();
				autoTileElement.add(element.detach());
			}
		}
		/* neccesary
		 * */
		List<Element> selectNodes = autoTileElement.selectNodes(".//*[text()]");
		for (Element element : selectNodes) {
			String replace = element.getText().replace("‘", "'").replace("’", "'");
			element.setText(replace);
		}
		Node detach = autoTileElement.detach();
		String asXML = detach.asXML();
		autoDocBody.add(detach);
		return asXML;
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
