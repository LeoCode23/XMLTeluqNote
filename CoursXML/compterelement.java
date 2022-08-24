import org.w3c.dom.*;
import javax.xml.parsers.*;

public class compterelement {

    // Compte tous les élément enfants d'un noeud donné et ajoute à un compteur
    public static int compteElementEnfant(Node noeud, int compteur) {
        if (noeud.getNodeType() == Node.ELEMENT_NODE) {
            compteur++;
        }
        NodeList listeEnfants = noeud.getChildNodes();
        for (int i = 0; i < listeEnfants.getLength(); i++) {
            compteur = compteElementEnfant(listeEnfants.item(i), compteur);
        }
        return compteur;
    }

    // fonction qui compte tous les éléments d'un document xml
    public static int compteElement(String fichier) {
        int nbElement = 0;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(fichier);
            NodeList liste = document.getChildNodes();
            for (int i = 0; i < liste.getLength(); i++) {
                Node noeud = liste.item(i);
                if (noeud.getNodeType() == Node.ELEMENT_NODE) {
                    nbElement++;
                    // comptes les noeuds enfants
                    nbElement += compteElementEnfant(noeud, 0);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return nbElement;
    }

    public static void main(String[] args) {
        String filename = "test.xml";
        System.out.println("Le document xml contient " + compteElement(filename) + " éléments");
    }

}
