import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class modifierencodage {


    // Modifie la déclaration de l'encodage de UTF-8 à ISO-8859-1 ----------------------------------------------------- 
    static void modifier_encodage(String filePath) {
        String ancien = "encoding=\"utf-8\"";
        String nouveau = "encoding=\"ISO-8859-1\"";
        File fileToBeModified = new File(filePath);
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(fileToBeModified));
            String line = reader.readLine();
            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();}
            String newContent = oldContent.replaceAll(ancien, nouveau);
            writer = new FileWriter(fileToBeModified);
            writer.write(newContent);
        } catch (IOException e) { e.printStackTrace(); } finally {
            try {
                reader.close();
                writer.close();
            } catch (IOException e) {e.printStackTrace();}}}
        // ------------------------------------------------------



            public static void main(String[] args) {
        String chemin = "copy.xml";

        modifier_encodage(chemin);

        System.out.println("done");
    }
}