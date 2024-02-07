/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.helpers;

import java.lang.reflect.InvocationTargetException;

public class Exceptions
{
    public static <T extends Throwable> T withCause( T exception, Throwable cause )
    {
        try
        {
            exception.initCause( cause );
        }
        catch ( Exception failure )
        {
            // ok, we did our best, guess there will be no cause
        }
        return exception;
    }

    public static RuntimeException launderedException( Throwable exception )
    {
        return launderedException( "Unexpected Exception", exception );
    }

    public static RuntimeException launderedException( String messageForUnexpected, Throwable exception )
    {
        if ( exception instanceof RuntimeException )
        {
            return (RuntimeException) exception;
        }
        else if ( exception instanceof Error )
        {
            throw (Error) exception;
        }
        else if ( exception instanceof InvocationTargetException )
        {
            return launderedException( messageForUnexpected,
                    ( (InvocationTargetException) exception ).getTargetException() );
        }
        else
        {
            throw new RuntimeException( messageForUnexpected, exception );
        }
    }

    private Exceptions()
    {
        // no instances
    }
}
