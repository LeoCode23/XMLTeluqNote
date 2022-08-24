import org.w3c.dom.*;
import javax.xml.parsers.*;

public class sommepaire {

    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        // String filename = args[0];
        String filename = "test.xml";
        Document doc = parser.parse(filename);
        Node racine = doc.getDocumentElement();

        // NodeListe des enfants de la balise racine
        NodeList liste = racine.getChildNodes();

        // Permet de stocker 4.9e-324 à 1.8e+308 (comme nous avons aucune idée de la
        // grandeur du fichier)
        double somme = 0;
        for (int i = 0; i < liste.getLength(); i++) {
            if (liste.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) liste.item(i);
                if (element.getTagName().equals("paire")) {
                    // Affiche l'addition des valeurs des éléments paire
                    System.out.println("Nombre: " + Double.parseDouble(element.getTextContent()));
                    somme += Double.parseDouble(element.getTextContent());
                }
            }
        }
        System.out.println("La somme des nombres est " + somme);
    }
}

