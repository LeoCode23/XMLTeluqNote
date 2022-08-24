
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
  



class fonction_navigateur {

    // --------------------------------------------------------------
    public static void ouvrir_nav(String URI) throws Exception{
    Desktop desk = Desktop.getDesktop();
    desk.browse(new URI(URI));
    }
    // --------------------------------------------------------------

    public static void main(String[] args)
             throws Exception
    {
        String url = "http://www.google.com";
        ouvrir_nav(url);
    }
}




