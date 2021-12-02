package org.sourceofevil.ldifcompare.model;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

public class LDAPEntities
{		
    private final HashMap<String, Entity> entities;

    public void addEntity(Entity entity)
    {
    	this.entities.put(entity.getDn(), entity);
    }
    
    public Entity getEntity(String dn)
    {
    	return this.entities.get(dn);
    }
    
    public boolean hasEntity(String dn)
    {
    	if ( this.entities.containsKey(dn) )
    		return true;
    	return false;
    }
    
    public Entity updateEntity(String dn, Date date, File file)
    {
    	if ( this.entities.containsKey(dn) )
    	{
    		entities.get(dn).update(date, file);
    		return entities.get(dn);
    	}
    	return null;
    }
    
    public Entity updateEntity(String dn, File file)
    {
    	if ( this.entities.containsKey(dn) )
    	{
    		entities.get(dn).update(file);
    		return entities.get(dn);
    	}
    	return null;
    }
    
    public boolean isEntityNewer(String dn, Date date)
    {
    	if ( this.entities.containsKey(dn) && ( entities.get(dn).getTimestamp() == null || entities.get(dn).getTimestamp().before(date) ) ) 
    		return true;
    	return false;
    }
    
	public boolean isEntityOlder(String dn, Date date)
    {
    	if ( this.entities.containsKey(dn) && ( entities.get(dn).getTimestamp() == null || entities.get(dn).getTimestamp().after(date) ) ) 
    		return true;
    	return false;
    }

    public boolean isEntitySame(String dn, Date date)
    {
    	if ( this.entities.containsKey(dn) && entities.get(dn).getTimestamp() != null && entities.get(dn).getTimestamp().equals(date) ) 
    		return true;
    	return false;
    }
    
	public LDAPEntities() 
	{
		super();
		this.entities = new HashMap<String,Entity>();
	}
	
	public int getSize()
	{
		return entities.size();
	}
}
