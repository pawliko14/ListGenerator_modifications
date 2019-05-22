package PDF;

import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;

import WB.DBConnection;

/*
 *  L$ - £o¿e
 *  %W$ - Wrzeciennik z³o¿enie
 *  W$ - Wrzeciennik korpus
 *  WR$ - Wrzeciono
 *  %K$ - Konik z³o¿enie
 *  K$ - Konik korpus
 *  SSZ$ - Sanie suportu wzd³u¿nego (sanki)
 *  SSX$ - Sanie suportu poprzecznego (szuflada)
 *  %P$ - Skrzynka posuwów z³o¿enie
 *  P$ - Skrzynka posuwów
 *  %S$ - Suport (zamek)
 *  S$ - Korpus zamka
 */

public class PDF_AnalizaStruktur {
	
	public static void run(){
		System.out.println("Start analizy struktur");
		try {
			Connection connection = DBConnection.dbConnector();
			Statement stmt = connection.createStatement();
		    String sql = "DELETE FROM Zlozenia";
		    stmt.executeUpdate(sql);
		    stmt.close();
		    
		    Statement pobierzProjectSchedule = connection.createStatement();
			String sql3 = "Select nrMaszyny, opis, dataprodukcji, komentarz  from Calendar where Zakonczone = 0 order by dataProdukcji";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql3);
			
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String komentarz = ProjectSchedule.getString("Komentarz");
				//pobranie detali z bonow magazynowych 
				String bony = "Select storenotesdetail.artikelcode, artikel_algemeen.omschrijving from storenotesdetail "
						+ "join artikel_algemeen on storenotesdetail.artikelcode = artikel_algemeen.artikelcode "
						+ "where projectnummer like '"+calyNumer+"%'";
				//ZMIANA NUMERU W KOMENTARZU
				if(komentarz.length()==8){
					bony=bony+" or projectnummer like '"+komentarz+"%' ";
					calyNumer = komentarz;
				}
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				//lista artyku³ów potrzebnych do maszyny
				Queue<String> art = new LinkedList<String>();
				
				//sprawdzenie zawartosci bonow
				bony = bony+ " and besteld <> 0 and leverancier = 102  ";
				Statement bony1 = connection.createStatement();
				ResultSet bony2 = bony1.executeQuery(bony);
				
				while(bony2.next()){
					String kod = bony2.getString("artikelcode");
					//detale charakterystyczne do listy montazowej oznaczone s¹ znakiem '$'
					String nazwa = bony2.getString("omschrijving");
					art.add(kod);
					
					//je¿eli znalazlo nazwe charakterystyczna
					if(nazwa.contains("$")){
						String[]s = null;
						s = nazwa.split("\\$");
						String rodzaj = s[0]+"$";
						
						//sprawdzamy czy jest
						Statement czyJest = connection.createStatement();
						String sprawdzanie = "SELECT projekt FROM Zlozenia WHERE projekt= '"+ProjectGroup+"/"+ProjectNumber+"' and typ = '"+rodzaj+"'";
						ResultSet wynikSprawdzenia = czyJest.executeQuery(sprawdzanie);
						boolean jest = false;
						while (wynikSprawdzenia.next()){
							jest=true;
						}
						czyJest.close();
						wynikSprawdzenia.close();
						
						//je¿eli nie ma to dodaj do tabeli zlozen
						if(!jest){
							Statement insert = connection.createStatement();
							// Projekt, typ, nr zamowienia, kod ar, nazwa, projekt nadrzedny, status
							//tutaj nr zamowienia i projekt nadrzedny puste
							insert.executeUpdate("INSERT INTO Zlozenia values "+
									"('"+ProjectGroup+"/"+ProjectNumber+"', "+
									"'"+rodzaj+"', "+
									"' ', "+
									"'"+kod+"', "+
									"'"+nazwa+"', " +
									"'"+ProjectNumber+"', 0, 0, 0)");
							insert.close();
						}
					}
				}
				bony2.close();
				bony1.close();
				
				
				//sprawdzenie zawartosci struktury podzespo³ów
				
				while(!art.isEmpty()){
					Statement st = connection.createStatement();
					String projektNadrz = art.remove();
					ResultSet rs1 = st.executeQuery("Select ONDERDEEL as kodArt, CFOMSONDERDEEL as nazwaArt, TYP from struktury where artikelcode ='"+projektNadrz+"' order by seq");
					while(rs1.next()){
						String kod = rs1.getString("kodArt");
						String nazwa = rs1.getString("nazwaArt");
						String typ = rs1.getString("TYP");
						if(nazwa == null) nazwa = "";
						art.add(kod);
						//NAZWA ARTYKULU
						if(nazwa.contains("$")&&!typ.equals("F")){
							String[]s = null;
							s = nazwa.split("\\$");
							String rodzaj = s[0]+"$";
							
							Statement czyJest = connection.createStatement();
							String sprawdzanie = "SELECT * FROM Zlozenia WHERE projekt= '"+ProjectGroup+"/"+ProjectNumber+"' and typ = '"+rodzaj+"'";
							ResultSet wynikSprawdzenia = czyJest.executeQuery(sprawdzanie);
							boolean jest = false;
							while (wynikSprawdzenia.next()){
								jest=true;
							}
							czyJest.close();
							wynikSprawdzenia.close();
							
							if(!jest){
								Statement insert = connection.createStatement();
								// Projekt, typ, nr zamowienia, kod ar, nazwa, projekt nadrzedny, status
								//tutaj nr zamowienia i projekt nadrzedny puste
								insert.executeUpdate("INSERT INTO Zlozenia values "+
										"('"+ProjectGroup+"/"+ProjectNumber+"', "+
										"'"+rodzaj+"', "+
										"' ', "+
										"'"+kod+"', "+
										"'"+nazwa+"', " +
										"'"+projektNadrz+"', 0, 0, 0)");
								insert.close();
							}
						}
					}
					st.close();
				}
			}
			pobierzProjectSchedule.close();
			System.out.println("Koniec analizy struktur");
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
}

