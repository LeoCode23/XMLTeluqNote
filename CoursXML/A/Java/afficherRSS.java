package Java;
import java.io.IOException;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class afficherRSS {

  public static void affichertitre(String URI) throws ParserConfigurationException, SAXException, IOException {
    //Permet de modifier rapidement le combo de noeud recherché
    String ITEM = "item";
    String TITRE = "title";
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder parser = factory.newDocumentBuilder();
    Document doc = parser.parse(URI);

    // Récupération de la liste des noeuds "item"
    NodeList items = doc.getElementsByTagName(ITEM);

    // Boucle jusqu'à la fin de la liste
    for (int i = 0; i < items.getLength(); i++) {
      Node noeud = items.item(i);
      Element eletitre = (Element) noeud;

      // Récupère une liste de noeud des titres du noeud courant item
      NodeList titleList = eletitre.getElementsByTagName(TITRE);
      Element titleElem = (Element) titleList.item(0);

      // Récupère le contenu du titre
      Node titleNode = titleElem.getChildNodes().item(0);
      System.out.println(titleNode.getNodeValue());
    }
  }

  public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
    String URI = args[0];
    affichertitre(args[0]);
  }
}
