package emailmanager;

import javax.servlet.http.HttpServlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.file.Matcher;

public class ListEmailServlet extends HttpServlet{

	private List<String> liste_adresses = new ArrayList<String>();
	String chemin_fichier;
	
	public ListEmailServlet() {
		super();
	}
	
	public void init() {
		/*** ouverture du fichier, lecture et chargement du contenu du fichier dans une structure dynamique (List)*****/
		//tout ajout, modification, suppression, affichage sera effectué a partir de la list. Le chargement du contenu de la liste dans le fichier txt sera effectué quand le servlet va etre dechargé (methode destroy())
		
		//recuperer le chemin du fichier
		chemin_fichier=this.getInitParameter("file_path");
		try {
			File fichier = new File(chemin_fichier);
			FileReader reader = new FileReader(fichier);
			BufferedReader buffer= new BufferedReader(reader);
			
			//mettre le contenu du fichier dans une list		
			String ligne; 
			while((ligne=buffer.readLine())!= null) {
				System.out.println("this is a line in the file: "+ligne);		
				liste_adresses.add(ligne);
			}	
			//libération les ressources associées à la lecture du fichier.
			buffer.close();
		}catch(IOException e){
			System.out.println(e.getMessage());
		}	
	}

	protected void subscribe(String email) throws EmailInvalideException, EmailExisteException{
		//verifier si l email est valide
		String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
		Pattern pattern = Pattern.compile(emailRegex);
		java.util.regex.Matcher matcher = pattern.matcher(email);
		if(!matcher.matches()){
			throw new EmailInvalideException();
		}
		//voir si l'adresse email entrée existe deja
		if(liste_adresses.contains(email)) {
			throw new EmailExisteException();

		}
		//ajouter l email si aucune exception n'est lancée
		liste_adresses.add(email);
	}
	
	protected void unsubscribe(String email)throws EmailExistePas ,EmailInvalideException{
		//verifier si l email est valide
		String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
		Pattern pattern = Pattern.compile(emailRegex);
		java.util.regex.Matcher matcher = pattern.matcher(email);
		if(!matcher.matches()){
			throw new EmailInvalideException();
		}
		// verifier si l'email a supprimer existe
		if(!liste_adresses.contains(email)) throw new EmailExistePas();
		//effacer l email si aucune exception n est lancee
		liste_adresses.remove(email);
	}
	
	private void save(List<String> addresses){
		try {
			File fichier = new File(chemin_fichier);
			FileWriter writer = new FileWriter(fichier);
			BufferedWriter buffer = new BufferedWriter(writer);
			//ecriture de chaque email dans une ligne dans le fichier .txt
			for(int i=0;i<addresses.size();i++) {
				buffer.write(addresses.get(i));
				buffer.newLine();
			}
			buffer.close();
		}catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		
		String redirected = req.getParameter("redirected"); //redirected==1 si l utilisateur est redirigé apres avoir saisi un email invalide, redirected==2 si il a saisi un email qui existe deja (pour l'ajout) , redirected==3 si il a saisi un email qui n existe pas (pour suppression)
		//System.out.println(redirected);
		PrintWriter out = res.getWriter();

        out.println("<HTML>");
        out.println("<head><title>gerer mail</title></head>");
        out.println("<body>");
		//affichage des emails inscrits a partir de liste_adresses
        out.println("Membres: ");
        out.println("<br>");
        out.println("<br>");
        out.println("<ol>");
        for (int i = 0; i < liste_adresses.size(); i++) {
            out.println("<li>"+liste_adresses.get(i)+"</li>");
        }
        out.println("</ol>");   
        //formulaire pour subscribe et unsubscribe
        out.println("<hr>");
        out.println("<p>Entrer votre adresse email: </p><form method='POST' action='ListEmailServlet' >");
        out.println("<input type=text name='email'/>");
        out.println("<input type='submit' name='action' value='subscribe'>");
        out.println("<input type='submit' name='action' value='unsubscribe'>");
        
        //ajouter un message si l utilisateur est redirigé pour resaisir l'email
        if (redirected != null) {
            switch (redirected) {
                case "1": //email invalide
                    out.println("<p style='color: red;'>Veuillez saisir une adresse email valide.</p>");
                    break;
                case "2": //email existe deja
                    out.println("<p style='color: red;'>Cette adresse email existe déjà, veuillez choisir une autre.</p>");
                    break;
                case "3": //email n existe pas
                    out.println("<p style='color: red;'>Cette adresse email n'existe pas</p>");
                    break;
            }
        }

        out.println("</body>");
        out.println("</HTML>");
	}
	
	
	protected void doPost(HttpServletRequest req, HttpServletResponse res)  throws ServletException, IOException{
		String action = req.getParameter("action"); //action==subscibe ou action==unsubscribe
		String email= req.getParameter("email");
		PrintWriter out= res.getWriter();
		res.setContentType("text/html");
		
		try {
			if(action.equals("subscribe")) {
				subscribe(email);
				out.println("L adresse email '"+email+"'  "+"est Ajoutée avec succes");
			}else {
				unsubscribe(email);
				out.println("L adresse email '"+email+"'  "+"est Supprimée avec succes");
			}
		}catch(EmailInvalideException e){
			//redirection de l'utilisateur pour resaisir le mail en ajoutant un parametre redirected=true pour pouvoir afficher le message
			res.sendRedirect(req.getContextPath()+"/ListEmailServlet?redirected=1"); //cas email invalide
			
		}catch(EmailExisteException e){
			res.sendRedirect(req.getContextPath()+"/ListEmailServlet?redirected=2"); //cas email existe deja - pour l ajout
			
		}catch(EmailExistePas e){
			res.sendRedirect(req.getContextPath()+"/ListEmailServlet?redirected=3"); //email n'existe pas - pour la suppression
		}

        out.println("<hr>");
        out.println("<a href='ListEmailServlet'>Retourner a la liste</a>");
	}
	
	public void destroy() {
		//enregistrement du contenu de la liste dans le fichier texte
		save(liste_adresses);
	}
	

}
