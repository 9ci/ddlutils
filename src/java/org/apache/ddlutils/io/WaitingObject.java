package org.apache.ddlutils.io;

import java.util.HashSet;

import org.apache.commons.beanutils.DynaBean;

/**
 * Represents an object waiting for insertion into the database. Is used by the
 * {@link org.apache.ddlutils.io.DataToDatabaseSink} to insert the objects in the correct
 * order according to their foreign keys.
 */
public class WaitingObject
{
    /** The object that is waiting for insertion */
    private DynaBean _obj;
    /** The identities of the waited-for objects */
    private HashSet _waitedForIdentites = new HashSet();

    /**
     * Creates a new <code>WaitingObject</code> instance for the given object.
     * 
     * @param obj The object that is waiting
     */
    public WaitingObject(DynaBean obj)
    {
        _obj = obj;
    }

    /**
     * Returns the waiting object.
     * 
     * @return The object
     */
    public DynaBean getObject()
    {
        return _obj;
    }

    /**
     * Adds the identity of another object that the object is waiting for.
     * 
     * @param fkIdentity The identity of the waited-for object
     */
    public void addPendingFK(Identity fkIdentity)
    {
        _waitedForIdentites.add(fkIdentity);
    }

    /**
     * Removes the specified identity from list of identities of the waited-for objects.
     * 
     * @param fkIdentity The identity to remove
     */
    public void removePendingFK(Identity fkIdentity)
    {
        _waitedForIdentites.remove(fkIdentity);
    }

    /**
     * Determines whether there are any identities of waited-for objects
     * registered with this waiting object.
     * 
     * @return <code>true</code> if identities of waited-for objects are registered
     */
    public boolean hasPendingFKs()
    {
        return !_waitedForIdentites.isEmpty();
    }
}
