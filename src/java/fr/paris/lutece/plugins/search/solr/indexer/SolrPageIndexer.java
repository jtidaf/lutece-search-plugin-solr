/*
 * Copyright (c) 2002-2009, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.search.solr.indexer;

import fr.paris.lutece.plugins.search.solr.business.SolrServerService;
import fr.paris.lutece.portal.business.page.Page;
import fr.paris.lutece.portal.business.page.PageHome;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.page.PageService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;

import org.apache.lucene.demo.html.HTMLParser;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;


/**
 * The indexer service for Solr.
 *
 */
public class SolrPageIndexer implements SolrIndexer
{
    private static final SolrServer SOLR_SERVER = SolrServerService.getInstance(  ).getSolrServer(  );
    private static final String SITE = AppPropertiesService.getProperty( "solr.site.name" );
    private static final String PARAMETER_PAGE_ID = "page_id";
    private static final String NAME = "SolrPageIndexer";
    private static final String VERSION = "1.0.0";
    private static final String TYPE = "PAGE";
    private static final String CATEGORIE = "Html";
    private static final String PROPERTY_PAGE_BASE_URL = "document.documentIndexer.baseUrl";
    private static final String SITE_URL = AppPropertiesService.getProperty( PROPERTY_PAGE_BASE_URL );
    private static final String PROPERTY_INDEXER_ENABLE = "solr.indexer.page.enable";

    /**
     * Creates a new SolrPageIndexer
     */
    public SolrPageIndexer(  )
    {
    }

    /**
     * Indexes data.
     * @return the logs.
     */
    public String index(  )
    {
        StringBuilder sbLogs = new StringBuilder(  );
        List<Page> listPages = PageHome.getAllPages(  );

        for ( Page page : listPages )
        {
            sbLogs.append( "indexing " );
            sbLogs.append( TYPE );
            sbLogs.append( " Id : " );
            sbLogs.append( page.getId(  ) );
            sbLogs.append( " Id : " );
            sbLogs.append( page.getName(  ) );
            sbLogs.append( "<br/>" );

            try
            {
                SolrItem item = getItem( page, SITE_URL );
                SOLR_SERVER.addBean( item );
            }
            catch ( IOException e )
            {
                AppLogService.error( e.getMessage(  ), e );
            }
            catch ( InterruptedException e )
            {
                AppLogService.error( e.getMessage(  ), e );
            }
            catch ( SiteMessageException e )
            {
                AppLogService.error( e.getMessage(  ), e );
            }
            catch ( SolrServerException e )
            {
                AppLogService.error( e.getMessage(  ), e );
            }
        }

        try
        {
            SOLR_SERVER.commit(  );
        }
        catch ( SolrServerException e )
        {
            AppLogService.error( e.getMessage(  ), e );
        }
        catch ( IOException e )
        {
            AppLogService.error( e.getMessage(  ), e );
        }

        return sbLogs.toString(  );
    }

    /**
    * Builds a document which will be used by Lucene during the indexing of the pages of the site with the following
    * fields : summary, uid, url, contents, title and description.
    * @return the built Document
    * @param strUrl The base URL for documents
    * @param page the page to index
    * @throws IOException The IO Exception
    * @throws InterruptedException The InterruptedException
    * @throws SiteMessageException occurs when a site message need to be displayed
    */
    private SolrItem getItem( Page page, String strUrl )
        throws IOException, InterruptedException, SiteMessageException
    {
        // the item
        SolrItem item = new SolrItem(  );

        // indexing page content
        String strPageContent = PageService.getInstance(  ).getPageContent( page.getId(  ), 0, null );
        StringReader readerPage = new StringReader( strPageContent );
        HTMLParser parser = new HTMLParser( readerPage );

        //the content of the article is recovered in the parser because this one
        //had replaced the encoded caracters (as &eacute;) by the corresponding special caracter (as ?)
        Reader reader = parser.getReader(  );
        int c;
        StringBuilder sb = new StringBuilder(  );

        while ( ( c = reader.read(  ) ) != -1 )
        {
            sb.append( String.valueOf( (char) c ) );
        }

        reader.close(  );

        item.setContent( sb.toString(  ) );
        item.setTitle( page.getName(  ) );
        item.setRole( page.getRole(  ) );

        if ( ( page.getDescription(  ) != null ) && ( page.getDescription(  ).length(  ) > 1 ) )
        {
            item.setSummary( page.getDescription(  ) );
        }

        item.setType( TYPE );
        item.setSite( SITE );

        List<String> cat = new ArrayList<String>(  );
        cat.add( CATEGORIE );
        item.setCategorie( cat );
        item.setDate( page.getDateUpdate(  ) );

        UrlItem urlItem = new UrlItem( strUrl );
        urlItem.addParameter( PARAMETER_PAGE_ID, page.getId(  ) );
        item.setUrl( urlItem.getUrl(  ) );
        item.setUid( item.getSite(  ) + item.getType(  ) + page.getId(  ) );

        return item;
    }

    /**
     * Returns the name of the indexer.
     * @return the name of the indexer
     */
    public String getName(  )
    {
        return NAME;
    }

    /**
     * Returns the version.
     * @return the version.
     */
    public String getVersion(  )
    {
        return VERSION;
    }

    public String getDescription(  )
    {
        return "Solr page Indexer";
    }

    public boolean isEnable(  )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }
}
