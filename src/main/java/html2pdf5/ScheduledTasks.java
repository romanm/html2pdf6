package html2pdf5;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.tidy.Tidy;

@Component
public class ScheduledTasks {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	Tidy tidy = getTidy();
	DOMReader domReader = new DOMReader();
	String domain = "http://workshop-manuals.com";
	//develop
//	String outputDir ="/home/roman/algoritmed.com/jura-boris/workshop-manuals.com";
	//prodaction
	String outputDir ="/home/holweb/jura/workshop-manuals.com";
	
	private Document autoIndexDocument;
	private Element bodyElAutoIndexDocument;
	private int cnt1 = 0, cnt2 = 0, cntVehicles = 0;
	
	@Scheduled(fixedRate = 500000000)
	public void reportCurrentTime() {
		System.out.println("The time is now " + dateFormat.format(new Date()));
		
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
//			if(cnt1 >=5)
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
				String autoName = autoElement.getText().trim();
				String replace = autoHref.replace("/", "");
				if(replace.equals(manufacturer) 
						|| autoHref.equals(domain)
						) continue;
				readAuto(autoHref, autoName, manufacturer);
				cnt2++;
				System.out.println("-----------------------------------------------------"+cnt2);
//				if(cnt2==5)
//					break;
			}
		}
	}

	private void readAuto(String autoHref, String autoName, String manufacturer) {
		logger.debug(autoHref);
		Document domFromStream = getDomFromStream(autoHref);

		Document autoDocument = createAutoDocument();
		Element autoDocBody = (Element) autoDocument.selectSingleNode("/html/body");
		autoDocBody.addElement("h1").addText(autoName);
		int autoTileNr = 1;
		logger.debug(autoHref+" :: "+autoName);
		addAutoTile(autoHref, autoTileNr, "t1", domFromStream, autoDocBody);

		List<Element> autoTileContextIndex = getAutoTileContextIndex(autoHref, domFromStream);
		logger.debug("tiles count "+autoTileContextIndex.size());
		for (Element element : autoTileContextIndex) {
			logger.debug(element.asXML());
			autoHref = element.attribute("href").getValue();
			String autoTileName = element.getText();
			if(autoHref.contains("privacy.php")
			|| autoHref.replaceAll("/", "").equals(manufacturer)
			|| autoHref.equals(domain)
			)
				continue;
			domFromStream = getDomFromStream(autoHref);
			autoTileNr++;
			addAutoTile(autoHref, autoTileNr, autoTileName, domFromStream, autoDocBody);
//			if(autoTileNr>=4)
//				break;
		}
		
		saveHtml(autoDocument, outputDir+"/"+manufacturer+"/"+autoName+".html");
//		readManualIndex(msg);
	}

	private void addAutoTile(String autoHref, int autoTileNr, String autoTileName, Document domFromStream, Element autoDocBody) {
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
			List<Element> breadcrum = autoTileElement2.selectNodes("div/h3");
			for (Element element : breadcrum) {
				element.detach();
			}
			autoTileElement = autoDocBody.addElement("div");
			autoTileElement.addAttribute("id","auto_tile_"+autoTileNr);
			
			for (Iterator iterator = autoTileElement2.elementIterator(); iterator.hasNext();) {
				Element element = (Element) iterator.next();
				autoTileElement.add(element.detach());
			}
		}
		autoDocBody.add(autoTileElement.detach());
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
		String msg = autoHref+""+indexHrefAdd;
		List<Element> selectNodes2 = getDomFromStream(msg).selectNodes("/html/body/div//a");
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
		org.w3c.dom.Document html2xhtml = tidy.parseDOM(requestBody, null);
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
