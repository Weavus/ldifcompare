package org.sourceofevil.ldifcompare.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Entity
{	
    private final String dn;
    private String md5;
	private String contents;
    private Date timestamp;
    private List<File> files;
    
	public Entity(String dn, Date timestamp, File file, String md5) 
	{
		super();
		this.dn = dn;
		this.timestamp = timestamp;
		this.md5 = md5;
		this.files = new ArrayList<>();
		files.add(file);
	}

	public final String getDn() 
	{
		return dn;
	}
	
	public final String getShortDn() 
	{
		if (dn != null)
		{
			String name = dn.substring(4);
			if ( name.indexOf(",") != -1 )
				name = name.substring(0,name.indexOf(","));
			return name;
		}
		return dn;
	}
	
	public final Date getTimestamp() 
	{
		return timestamp;
	}
	
	public final List<File> getFiles() 
	{
		return files;
	}

	public final void setTimestamp(Date timestamp) 
	{
		this.timestamp = timestamp;
	}

	public final void addFile(File file) 
	{
		files.add(file);
	}
	
	public final void resetFiles(File file) 
	{
		this.files.clear();
		files.add(file);
	}
	
	public final void update(Date updated, File file)
	{
		if (this.timestamp == null || this.timestamp.before(updated) )
		{
			this.timestamp = updated;
			resetFiles(file);
		}
		else if ( this.timestamp.equals(updated) )
		{
			addFile(file);
		}
	}
	
	public final void update(File file)
	{
		addFile(file);
	}
	
	public final String toString()
	{
		return getShortDn() + " Modified: " + this.timestamp + " Found In: " + files.toString();
	}

	public final String getMd5() 
	{
		return md5;
	}

	public final void setMd5(String md5) 
	{
		this.md5 = md5;
	}	

	public final String getContents() 
	{
		return contents;
	}

	public final void setContents(String contents) 
	{
		this.contents = contents;
	}

}
