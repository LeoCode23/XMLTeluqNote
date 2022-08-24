 /**
* INF 6450 -  Travail noté 4 - Léo Talbot,  fait avec JDK 17.0.3
* javac xpathsearch.java
* java xpathsearch inventaire.xml 32
*/
import javax.xml.parsers.*;
 import javax.xml.xpath.*;
import org.w3c.dom.*;


public class xpathsearch {


    public static void main(String[] args) throws Exception {

// Variable de départ
//String filename = "inventaire.xml";
//String code = "321";
String filename = args[0];
String code = args[1];


/* on doit construire une instance du document XML */
 DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
 DocumentBuilder builder = dbfact.newDocumentBuilder();
 /* on peut traiter directement un URL */
 Document document = builder.parse(filename);
 /* on construit un objet XPath */
 XPathFactory fact = XPathFactory.newInstance();
 XPath xpath = fact.newXPath();
 /* finalement, on peut émettre sa requête XPath sur le document */
 String title = xpath.evaluate("//inventaire/produit[@code=" + code + "]/@prix", document);
    System.out.println("Le prix est de " + title + " $");
}
    
}
