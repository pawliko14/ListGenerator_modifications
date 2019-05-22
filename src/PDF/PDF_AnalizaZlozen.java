package PDF;

import java.sql.*;

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


public class PDF_AnalizaZlozen {

	public static void run(Connection connection){
		try 
		{
			
			System.out.println("start analizy z³o¿eñ");
			Statement pobierzProjectSchedule = connection.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, Komentarz from calendar where zakonczone = 0 order by dataProdukcji";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			//dla ka¿dego projektu
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				
				//dla wszystkich artyku³ów z '$' dla danego projektu:
				Statement takeZloz = connection.createStatement();
				String sql1 = "Select kodArtykulu, typ from Zlozenia where projekt = '"+calyNumer+"'";
				ResultSet rs2 = takeZloz.executeQuery(sql1);
				while(rs2.next()){
					
					String sql2 = "SELECT MatSource, ItemNo, ItemDesc, ConsumerOrder as ProjektNadrzedny FROM partsoverview " +
							"where `OrderNo` = '"+calyNumer+"' AND ItemNo = '"+rs2.getString("kodArtykulu")+"'";
					/*if(zmiana)
						sql2 = "SELECT MatSource, ItemNo, ItemDesc, ConsumerOrder as ProjektNadrzedny FROM partsoverview " +
								"where `OrderNo` = '"+zmianaNumeru+"' AND ItemNo = '"+rs2.getString("kodArtykulu")+"'";*/
					Statement take = connection.createStatement();
					ResultSet rs = take.executeQuery(sql2);
					int status = 90;
					String zamowienie="";
					String rodzaj = rs2.getString("typ");
					while(rs.next()){
						String nazwa = rs.getString("ItemDesc");
						status = 10;
						int i_status = 0;
						if(nazwa.contains("$")){
							//pobiera MatSource z partsoverview dla danego artyku³u
							zamowienie = rs.getString("MatSource");
							//jeœli artyku³ jest zamówiony na produkcji (piêæsetka)
							if(zamowienie.startsWith("500/")){
								//System.out.println("pobierze technologie");
								String number = zamowienie.substring(4, zamowienie.length());
								Statement TakeTechn = connection.createStatement();
								//sprawdzamy jego stan w postêpie produkcji
								String sql3 = "SELECT werkpost, status from werkbon " +
										"where Afdeling = '500' and Afdelingseq='"+number+"' order by seq";
								ResultSet technology = TakeTechn.executeQuery(sql3);
								status = 0;
								while(technology.next())
								{
									i_status = technology.getInt("status");
									if(status==0||status==10)
										status = i_status;
									else if(status==90 && i_status!=90){
										status=20;
									}
									
								}
								TakeTechn.close();
							}
							else if(zamowienie.startsWith("Na mag")){
								status = 90;
							}
						}
					}
					Statement ps = connection.createStatement();
					//update statusu zlozenia:
					if(!zamowienie.equals("")){
						// create our java preparedstatement using a sql update query
					    
					    String sql5 = "UPDATE Zlozenia SET nrZamowienia = '"+zamowienie+"', status = '"+status+"' WHERE projekt =  '"+ProjectGroup+"/"+ProjectNumber+"' AND typ = '"+rodzaj+"'";
					    ps.executeUpdate(sql5);
					}
					else{
						// create our java preparedstatement using a sql update query
						 String sql5 = "UPDATE Zlozenia SET status = '"+status+"' WHERE projekt =  '"+ProjectGroup+"/"+ProjectNumber+"' AND typ = '"+rodzaj+"'";
						  
					    ps.executeUpdate(sql5);
					}
					// call executeUpdate to execute our sql update statement
				    ps.close();
				    take.close();
				}
				takeZloz.close();
				
			}
			pobierzProjectSchedule.close();
			System.out.println("Koniec analizy");
			
			System.out.println("Sprawdzenie kompletnoœci czêœci");
			
			//wybranie wszystkich serii 500
			Statement st = connection.createStatement();
			String sql2 = "Select projekt, nrZamowienia from Zlozenia where nrZamowienia like '500%' group by nrZamowienia";
			ResultSet rs = st.executeQuery(sql2);
			while(rs.next()){
				//dla kazdej serii sprawdzenie, czy w partsoverview siê dla danej 500 cos jeszcze robi
				//statement wybiera ile jest jeszcze czesci robionych dla tej 500
				//jesli juz nic nie ma to ilosc czesci podrzednych jest == 0
				String nr500 = rs.getString(2);
				String projekt = rs.getString(1);
				Statement st2 = connection.createStatement();
				String sql3 = "Select matsource, ItemNo, storenotesdetail.leverancier from PartsOverview "
						+ " join storenotesdetail on storenotesdetail.projectnummer = partsoverview.consumerorder and storenotesdetail.artikelcode = partsoverview.itemno "
						+ "where ConsumerOrder = '"+nr500+"' and MatSource <> 'Na magazynie' and orderno = '"+projekt+"' AND STORENOTESDETAIL.BESTELD <>0";
				ResultSet rs2 = st2.executeQuery(sql3);
				int ilosc = 0;
				boolean p102 = true, p103 = true;
				while(rs2.next()){
					if(rs2.getString("Leverancier").equals("102"))	p102 = false;
					if(rs2.getString("Leverancier").equals("103"))	p103 = false;
				}
				rs2.close();
				st2.close();
				//jesli ilosc czesci podrzednych == 0 to mozna postawic ticka, ze wszystko jest gotowe
				if(p102||p103){
					Statement st3 = connection.createStatement();
					String sql4 = "Update Zlozenia set ";
					if(p102 && !p103)
						sql4+="doWydania102 = 1 ";
					else if (!p102 && p103)
						sql4+="doWydania103 = 1 ";
					else if (p102 && p103)
						sql4+="doWydania102 = 1, doWydania103 = 1 ";
					sql4+= " where nrZamowienia = '"+nr500+"' and projekt = '"+projekt+"'";
					st3.executeUpdate(sql4);
					st3.close();
				}
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
}