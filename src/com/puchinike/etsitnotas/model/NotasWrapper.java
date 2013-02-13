package com.puchinike.etsitnotas.model;

import java.util.Calendar;

public class NotasWrapper {
	private Asignatura[] asignaturas;
	private String name;
	private String lastupdate;
	
	public NotasWrapper(Asignatura[] asignaturas, String name) {
		this.asignaturas = asignaturas;
		this.name = name;
		String date = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		this.lastupdate = "Última actualización:\r\n" + date;
	}
	
	public String getLastupdate() {
		return lastupdate;
	}
	public void setLastupdate(String lastupdate) {
		this.lastupdate = lastupdate;
	}
	public String getName() {
		return name;
	}
	public Asignatura[] getAsignaturas() {
		return asignaturas;
	}
}
