package client;

import javax.swing.table.AbstractTableModel;

import PdiRemoteAuth.User;

public class UsersListModel extends AbstractTableModel {

	private static final long serialVersionUID = 3402467991064037193L;
	User[] users = null;
	
	public int getColumnCount() {
		return(3);
	}

	public int getRowCount() {
		if (users == null) return(0);
		return(users.length);
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (users != null) {
			if (columnIndex == 0) {
				return(users[rowIndex].username);
			}
			
			if (columnIndex == 1) {
				return(users[rowIndex].password);
			}
			
			if (columnIndex == 2) {
				return(users[rowIndex].rights);
			}
		}
		return(null);
	}
	
	public String getColumnName(int index) {
		if (index == 0) { return("Jméno"); }
		if (index == 1) { return("Heslo"); }
		if (index == 2) { return("Práva"); }
		return(""+index);
	}
	
	public void setData(User[] data) {
		users = data;
		fireTableDataChanged();
	}
	
	public void cleanData() {
		users = null;
		fireTableDataChanged();
	}

}
