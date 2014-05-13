buddycloud-media-server
=======================

A media server for [buddycloud][bc] channels. It provides a simple 
REST-like HTTP interface where clients can do several operations like:
- **upload**: upload media to private/public channels, the permissions
are based on channels [pubsub subscriptions][pubsub];
- **download**: download/visualize media from channels that you have 
enough permissions;
- **delete**: you can delete medias that you've uploaded or if you are
a channel moderator/owner;
- **update**: update media metadata, with similar permissions as the
delete operation.
 
To authenticate HTTP requests, the media server uses [XEP-0070][xep],
this means that the client **must** have an XMPP client that "understands"
such protocol in order to do media requests. There is only one exception: 
download media from public channels - any client has access.

[bc]: http://buddycloud.com/
[xep]: http://xmpp.org/extensions/xep-0070.html
[pubsub]: https://buddycloud.org/wiki/XMPP_XEP#Follower_Management

Usage
-----

#### Via the Buddycloud API

The API endpoints are described in detail [here](https://buddycloud.org/wiki/Buddycloud_HTTP_API#.2F.3Cchannel.3E.2Fmedia.2F.3Citem.3E).

#### Using the Media Server directly

##### Discovering the HTTP endpoint

In order to figure out which endpoint to send HTTP calls to, we use [XMPP Service Discovery] (http://xmpp.org/extensions/xep-0030.html) against the domain running the media server. In the folowing example, we use buddycloud.org as the target domain. We first list all services provided by buddycloud.org and then we pick the one with name "Media Server".

disco#items against buddycloud.org:
```xml
<iq to="buddycloud.org" type="get">
  <query xmlns="http://jabber.org/protocol/disco#items" />
</iq>

<iq type="result" from="buddycloud.org">
  <query xmlns="http://jabber.org/protocol/disco#items">
    <item jid="mediaserver.buddycloud.org" />
    <item jid="directory.buddycloud.org" />
    <item jid="channels.buddycloud.org" />
    <item jid="topics.buddycloud.org" />
    <item jid="search.buddycloud.org" />
    <item jid="anon.buddycloud.org" />
  </query>
</iq>
```

disco#info against mediaserver.buddycloud.org:
```xml
<iq to="mediaserver.buddycloud.org" type="get">
  <query xmlns="http://jabber.org/protocol/disco#info" />
</iq>

<iq type="result" from="mediaserver.buddycloud.org">
  <query xmlns="http://jabber.org/protocol/disco#info">
    <identity category="component" type="generic" name="Media Server" />
    <feature var="http://jabber.org/protocol/disco#info" />
    <feature var="urn:xmpp:ping" />
    <feature var="jabber:iq:last" />
    <feature var="urn:xmpp:time" />
    <x xmlns="jabber:x:data" type="result">
      <field var="FORM_TYPE" type="hidden"><value>http://buddycloud.org/v1/api</value></field>
      <field var="endpoint" type="text-single"><value>https://demo.buddycloud.org/api/media-proxy</value></field>
    </x>
  </query>
</iq>
```

The response of the disco#info query against the media server contains a dataform that holds information on how to communicate with the server. Then field named 'endpoint' will then give us the endpoint for HTTP calls. In the previous example, we should use **https://demo.buddycloud.org/api/media-proxy**.

##### Generating a transaction id

As per [XEP 0070](http://www.xmpp.org/extensions/xep-0070.html), every transaction with the media server that requires authentication must have an unique identifier within the context of the client's interaction with the server. This identifier will be sent over to the media server via HTTP and then sent back to the client via XMPP in order to confirm the client's identity.

For this example, we will use **a7374jnjlalasdf82** as a transaction id.

##### Listening for confirmation

Before sending the actual HTTP request, the client has to setup an XMPP listener for the confirmation request. The stanza sent by the media server complies with [XEP 0070](http://www.xmpp.org/extensions/xep-0070.html) and will be in the lines of:

```xml
<iq type='get' 
    from='mediaserver.buddycloud.org' 
    id='1'>
  <confirm xmlns='http://jabber.org/protocol/http-auth'
           id='a7374jnjlalasdf82'
           method='GET'/>
</iq>
```

The client should simply confirm the request by replying to the stanza:

```xml
<iq type='result' 
    to='mediaserver.buddycloud.org' 
    id='1' />
```

##### Sending the HTTP request

In order to build the URL for HTTP requests, we must consider the media endpoint, the channel the media was (or will be) posted and the media id (in case of GET or DELETE).

The requests that require authentication must go with an authorization header, which should be the concatenation of the client's full jid, plus a ':', plus the transaction id, converted to base64 (URL safe). I.e., let's assume the client's full jid is media-user@example.com/media-resource and the transaction id is, again, a7374jnjlalasdf82:

Authorization: urlbase64('media-user@example.com/media-resource:a7374jnjlalasdf82')
Authorization: bWVkaWEtdXNlckBleGFtcGxlLmNvbS9tZWRpYS1yZXNvdXJjZTphNzM3NGpuamxhbGFzZGY4Mg==

Note: the urlbase64 method should comply with http://tools.ietf.org/html/rfc4648#page-7. In python, for instance, that's base64.urlsafe_b64encode(s).

The following curl examples perform media-related operations within the media-channel@example.com channel.

**GET**

**DELETE**

**POST**

Setup
-----

The server is written on top of Java using [RESTlet](http://www.restlet.org/).

It uses [Maven](http://maven.apache.org/) to build its packages. You can build
the package manually or download it from [here](https://github.com/downloads/buddycloud/buddycloud-media-server/buddycloud-media-server-0.1.zip).

After unpacking, you can then start it by invoking

    bash mediaserver

The server needs to be configured to point to a Buddycloud and XMPP server. 
See the *Configuration* section.

Configuration
-------------

You can configure the media server by copying `mediaserver.properties.example` to 
`mediaserver.properties` in the server's root directory, and then editing as 
required. This file has multiple properties definitions:

        # HTTP 
        http.port=8080
        http.tests.port=9090
        http.endpoint=https://api.buddycloud.org/media-proxy
        https.port=8443
        https.enabled=true
        https.keystore.path=/$HOME/.jetty/jetty.jks
        https.keystore.type=JSK
        https.keystore.password=password
        https.key.password=password
        
        # XMPP
        xmpp.component.host=localhost
        xmpp.component.port=5275
        xmpp.component.subdomain=mediaserver.example.com
        xmpp.component.secretkey=secret
        
        xmpp.connection.username=mediaserver-test
        xmpp.connection.password=mediaserver-test
        xmpp.connection.host=localhost
        xmpp.connection.port=5222
        xmpp.connection.servicename=example.com 
        
        # How much time it will wait for a response to an XMPP request (in milliseconds)
        xmpp.reply.timeout=30000
        
        # JDBC
        jdbc.db.url=jdbc:postgresql://localhost:5432/mediaserver?user=postgres&password=postgres
        jdbc.driver.class=org.postgresql.Driver
        
        # Max threshold beyond which files are written directly to disk, in bytes
        # Only used while uploading multipart form data files
        media.todisk.threshold=1048576
        
        # File System
        media.storage.root=/tmp
        media.sizelimit=1000240
        
The following configuration options are supported:

HTTP related configurations:

- **http.endpoint** (Optional): if provided the HTTP endpoint of the media server will be advertised via DISCO#info using XEP-0128 Service Discovery Extensions.
- **https.enabled** (Optional): if the HTTPS is enabled (default is **false**). If is set to **true**
you **must** provide the others *https* properties.
- **https.port**: the port where the server will listen for HTTPS requests.
- **https.keystore**: the HTTPS keystore location.
- **https.keystore.type**: the keystore type.
- **https.keystore.password**: the keystore password.
- **https.key.password**: the HTTPS key password.
- **http.port** (Optional): the HTTP port where the server will listen for HTTP requests (default is *8080*).
- **http.tests.port** (Optional): the HTTP port where the server will listen for HTTP requests while running tests (default is *9090*).


XMPP related:

- **xmpp.component.host** (Required): the XMPP server location where the media server's component will connect. 
- **xmpp.component.port** (Required): the XMPP server components connection listening port.
- **xmpp.component.subdomain** (Required): the *subdomain* that will be used by the component.
- **xmpp.component.secretkey** (Required): the *secretkey* defined at the XMPP server for components connections.
	
In addition of the component, the media server also have a simple client that handles pubsub queries: 

- **xmpp.connection.username** (Required): the *username* used by the cient's connection.
- **xmpp.connection.password** (Required): client's connection *password*.
- **xmpp.connection.host** (Required): XMPP server location.
- **xmpp.connection.port** (Required): XMPP server port for clients connections.
- **xmpp.connection.servicename** (Required): client's connection *servicename*.

- **xmpp.reply.timeout** (Optional): timeout in milliseconds to wait a response to an XMPP request (default is 30000)

Storage related:

- **jdbc.db.url** (Required): the server uses [PostgresSQL](http://www.postgresql.org) to store
media's metadata and uses [JDBC](http://www.oracle.com/technetwork/java/overview-141217.html) to access it.
- **jdbc.driver.class** (Optional): if someday the media server allow a different database, this
property will be used (default is *org.postgresql.Driver*).
- **media.storage.root** (Required): root path where the media server will store the media files.
- **media.sizelimit** (Optional): the tolerated file content size which the media server will store (default is *104857600* - 100 MB).
- **media.todisk.threshold** (Optional): the tolerated file size in bytes (default is *1048576* - 1 MB) which beyond are directly stored on disk.
