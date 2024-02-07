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
package org.neo4j.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.neo4j.helpers.collection.MapUtil.stringMap;
import static org.neo4j.kernel.Config.ENABLE_REMOTE_SHELL;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.Config;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.shell.impl.SameJvmClient;
import org.neo4j.shell.impl.ShellBootstrap;
import org.neo4j.shell.impl.ShellServerExtension;
import org.neo4j.shell.kernel.GraphDatabaseShellServer;

public class ShellTest
{
    private AppCommandParser parse( String line ) throws Exception
    {
        return new AppCommandParser( new GraphDatabaseShellServer( null ),
            line );
    }

    @Test
    public void testParserEasy() throws Exception
    {
        AppCommandParser parser = this.parse( "ls -la" );
        assertEquals( "ls", parser.getAppName() );
        assertEquals( 2, parser.options().size() );
        assertTrue( parser.options().containsKey( "l" ) );
        assertTrue( parser.options().containsKey( "a" ) );
        assertTrue( parser.arguments().isEmpty() );
    }

    @Test
    public void testParserArguments() throws Exception
    {
        AppCommandParser parser = this
            .parse( "set -t java.lang.Integer key value" );
        assertEquals( "set", parser.getAppName() );
        assertTrue( parser.options().containsKey( "t" ) );
        assertEquals( "java.lang.Integer", parser.options().get( "t" ) );
        assertEquals( 2, parser.arguments().size() );
        assertEquals( "key", parser.arguments().get( 0 ) );
        assertEquals( "value", parser.arguments().get( 1 ) );

        assertException( "set -tsd" );
    }

    @Test
    public void testEnableRemoteShell() throws Exception
    {
        int port = 8085;
        GraphDatabaseService graphDb = new EmbeddedGraphDatabase(
            "target/shell-neo", stringMap( ENABLE_REMOTE_SHELL, "port=" + port ) );
        ShellLobby.newClient( port );
        graphDb.shutdown();
    }

    @Test
    public void testEnableServerOnDefaultPort() throws Exception
    {
        GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "target/shell-neo", MapUtil.stringMap( Config.ENABLE_REMOTE_SHELL, "true" ) );
        try
        {
            ShellLobby.newClient();
        }
        finally
        {
            graphDb.shutdown();
        }
    }
    
    @Test
    public void canConnectAsAgent() throws Exception
    {
        Integer port = Integer.valueOf( 1234 );
        String name = "test-shell";
        GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "target/shell-neo" );
        try
        {
            new ShellServerExtension().loadAgent( new ShellBootstrap( port.toString(), name ).serialize() );
        }
        finally
        {
            graphDb.shutdown();
        }
        ShellLobby.newClient( port.intValue(), name );
    }

    @Test
    public void testRemoveReferenceNode() throws Exception
    {
        final GraphDatabaseShellServer server = new GraphDatabaseShellServer( "target/shell-neo", false, null );
        ShellClient client = new SameJvmClient( server );
        
        Documenter doc = new Documenter("sample session", client);
        doc.add("pwd", "", "where are we?");
        doc.add("set name \"Jon\"", "", "On the current node, set the key \"name\" to value \"Jon\"");
        doc.add("start n=(0) return n", "peter", "send a cypher query");
        doc.add("mkrel -c -d i -t LIKES --np \"{'app':'foobar'}\"", "", "make an incoming relationship of type LIKES, create the end node with the node properties specified.");
        doc.add("ls", "1", "where are we?");
        doc.add("cd 1", "", "change to the newly created node");
        doc.add("ls -avr", "LIKES", "list relationships, including relationshship id");
        
        doc.add( "mkrel -c -d i -t KNOWS --np \"{'name':'Bob'}\"", "", "create one more KNOWS relationship and the end node" );
        doc.add( "pwd", "0", "print current history stack" );
        doc.add( "ls -avr", "KNOWS", "verbose list relationships" );
        doc.run();
        //TODO: implement support for removing root node and previous nodes in the history stack of PWD
        //client.getServer().interpretLine( "rmrel -d 0", client.session(), client.getOutput() );
//        client.getServer().interpretLine( "cd", client.session(), client.getOutput() );
//        client.getServer().interpretLine( "pwd", client.session(), client.getOutput() );
        server.shutdown();
    }

    private void assertException( String command )
    {
        try
        {
            this.parse( command );
            fail( "Should fail" );
        }
        catch ( Exception e )
        {
            // Good
        }
    }
}