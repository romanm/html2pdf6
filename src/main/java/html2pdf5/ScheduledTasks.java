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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
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
				readAuto(autoHref, autoName, manufacturer);
				cnt2++;
				System.out.println("-----------------------------------------------------"+cnt2);
				if(cnt2==1)
					break;
			}
		}
	}

	private void readAuto(String autoTileHref, String autoName, String manufacturer) {
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
		String addAutoTile = "123";
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
			System.out.println(
					autoTileNr+" -- "+autoTileName+
					"\n"+autoTileHref
					);
			String[] split = autoTileName.split(" - ");
			for (String string : split) {
				System.out.print(" -:- "+string);
			}
			System.out.println("");
			if(!autoTileH2.equals(split[0]))
			{
				autoDocBody.addElement("h2").addAttribute("data-bookmark", "h2").addElement("a").addAttribute("name", "h2_"+autoTileNr).addText(split[0]);
				autoTileH2 = split[0];
			}
			if(split.length > 1)
			{
				autoDocBody.addElement("h3").addAttribute("data-bookmark", "h3").addElement("a").addAttribute("name", "h3_"+autoTileNr).addText(split[1]);
				/*
				autoDocBody.addElement("a").addAttribute("href", autoTileHref).addText(split[1]);
				 * */
				domFromStream = getDomFromStream(autoTileHref);
				Element autoTileElement = (Element) domFromStream.selectSingleNode("/html/body//div[@id='page1-div']");
				if(autoTileElement != null){
					Element h4El = (Element) autoTileElement.selectSingleNode("p");//.detach();
					Node h4ElText = h4El.selectSingleNode("text()").detach();
					logger.debug(""+h4ElText.asXML());
					h4El.addAttribute("data-bookmark", "h4").addElement("a").addAttribute("name", "h4_"+autoTileNr).addText(h4ElText.getText());
//					autoDocBody.addElement("h4").addAttribute("class", "bookmark").addElement("a").addAttribute("name", "h4_"+autoTileNr).addText(h4El.getText());
//					autoTileElement.attribute("id").setValue("auto_tile_"+autoTileNr);
					changeImgUrl(autoTileElement);
					
					Node detach = autoTileElement.detach();
					autoDocBody.add(detach);
				}
			}
		}
		if(false)
		{
			
			for (Element element : autoTileContextIndex) {
				autoTileHref = element.attribute("href").getValue();
				String autoTileName = element.getText();
				if(autoTileHref.contains("privacy.php")
						|| autoTileHref.replaceAll("/", "").equals(manufacturer)
						|| autoTileHref.equals(domain)
						)
					continue;
				logger.debug("\n"+autoTileHref+"\n"+autoTileName);
				domFromStream = getDomFromStream(autoTileHref);
				autoTileNr++;
				addAutoTile = addAutoTile(autoTileHref, autoTileNr, autoTileName, domFromStream, autoDocBody);
				
				if(autoTileNr>=3)
					break;
			}
		}
		
		try{
			System.out.println("------buildBookmark----------------------");
			buildBookmark(autoDocument);
			String htmlOutFileName = outputDir+"/"+manufacturer+"/"+manufacturer+"_"+autoName+".html";
			System.out.println("----saveHtml------------------------");
			saveHtml(autoDocument, htmlOutFileName);
			System.out.println("------savePdf----------------------");
			savePdf(htmlOutFileName, htmlOutFileName+".pdf");
			System.out.println("------OK----------------------");
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("-NOT SAVED----manufacturer----------autoName-----------"
					+ autoTileNr
					+ "--"+autoTileContextIndex.size());
			System.out.println(addAutoTile);
			System.out.println(e.getMessage());
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
		//List<Element> bookmarkEls = autoDocument.selectNodes("/html/body/*[@class='bookmark']");
//		List<Element> bookmarkEls = autoDocument.selectNodes("/html/body//*[@data-bookmark='h2'|@data-bookmark='h3'|@data-bookmark='h4']");
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
			System.out.println(i);
			if(h234Element.attribute("data-bookmark").getValue().equals("h2"))
			{
				h2 = bookmarks.addElement("bookmark");
				h2id = "bm"+h2i++;
				makeBookmark(h2, h234Element, h2id);
				System.out.println(h2.asXML());
			}
			else
				if(h234Element.attribute("data-bookmark").getValue().equals("h3"))
				{
					h3 = h2.addElement("bookmark");
					h3id = h2id + "."+h3i++;
					makeBookmark(h3, h234Element, h3id);
					System.out.println(h3.asXML());
				}
				else
					if(h234Element.attribute("data-bookmark").getValue().equals("h4"))
					{
						h4 = h3.addElement("bookmark");
						h4id = h3id + "."+h4i++;
						makeBookmark(h4, h234Element, h4id);
						System.out.println(h4.asXML());
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
		Element selectSingleNode = (Element) domFromStream.selectSingleNode("/html/body//iframe");
		String indexHrefAdd = selectSingleNode.attributeValue("src");
		String hrefIndex = autoHref+""+indexHrefAdd;
		logger.debug("\n hrefIndex = "+hrefIndex);
		List<Element> selectNodes2 = getDomFromStream(hrefIndex).selectNodes("/html/body/div//a");
		return selectNodes2;
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

	private Document getDomFromStream(String url) {
		HttpURLConnection urlConnection = getUrlConnection(url);
		try {
			return getDomFromStream(urlConnection);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private Document getDomFromStream(HttpURLConnection urlConnection) throws IOException {
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
		Document document = domReader.read(html2xhtml);
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
