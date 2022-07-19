import org.w3c.dom.*;
import javax.xml.parsers.*;

 public class test {
    public static void main(String[] args) throws Exception {
       DocumentBuilderFactory factory = 
        DocumentBuilderFactory.newInstance();
       DocumentBuilder parser = factory.newDocumentBuilder();
       Document doc = parser.parse(args[0]); 
       Element racine = doc.getDocumentElement(); 
       System.out.println(racine.getTagName()); 
    }
 }