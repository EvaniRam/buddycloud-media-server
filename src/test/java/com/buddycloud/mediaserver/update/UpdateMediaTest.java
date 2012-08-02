package com.buddycloud.mediaserver.update;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.html.FormData;
import org.restlet.ext.html.FormDataSet;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import com.buddycloud.mediaserver.MediaServerTest;
import com.buddycloud.mediaserver.business.model.Media;
import com.buddycloud.mediaserver.commons.Constants;
import com.buddycloud.mediaserver.commons.MediaServerConfiguration;

public class UpdateMediaTest extends MediaServerTest {
	
	public void testTearDown() throws Exception {
		FileUtils.cleanDirectory(new File(configuration.getProperty(MediaServerConfiguration.MEDIA_STORAGE_ROOT_PROPERTY) + 
				File.separator + BASE_CHANNEL));
		
		dataSource.deleteMedia(MEDIA_ID);
	}
	
	@Override
	protected void testSetUp() throws Exception {
		File destDir = new File(configuration.getProperty(MediaServerConfiguration.MEDIA_STORAGE_ROOT_PROPERTY) + File.separator + BASE_CHANNEL);
		if (!destDir.mkdir()) {
			FileUtils.cleanDirectory(destDir);
		}
		
		FileUtils.copyFile(new File(TESTFILE_PATH + TESTIMAGE_NAME), new File(destDir + File.separator + MEDIA_ID));
		
		Media media = buildMedia(MEDIA_ID, TESTFILE_PATH + TESTIMAGE_NAME);
		dataSource.storeMedia(media);
	}
	
	@Test
	public void anonymousSuccessfulUpdate() throws Exception {
		// file fields
		String title = "New Image";
		String description = "New Description";

		ClientResource client = new ClientResource(BASE_URL + "/" + BASE_CHANNEL + "/media/" + MEDIA_ID);
		client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, BASE_USER, BASE_TOKEN);

		FormDataSet form = new FormDataSet();
		form.setMultipart(true);
		form.getEntries().add(new FormData(Constants.TITLE_FIELD,
		new StringRepresentation(title)));	
		form.getEntries().add(new FormData(Constants.DESC_FIELD,
		new StringRepresentation(description)));
		
		Representation result = client.post(form);
		Media media = gson.fromJson(result.getText(), Media.class);

		// verify if resultant media has the passed attributes
		assertEquals(title, media.getTitle());
		assertEquals(description, media.getDescription());
	}
	
	@Test
	public void anonymousSuccessfulUpdateParamAuth() throws Exception {
		// file fields
		String title = "New Image";
		String description = "New Description";

		Base64 encoder = new Base64(true);
		String authStr = BASE_USER + ":" + BASE_TOKEN;
		
		ClientResource client = new ClientResource(BASE_URL + "/" + BASE_CHANNEL + "/media/" + MEDIA_ID +
				"?auth=" + new String(encoder.encode(authStr.getBytes())));

		FormDataSet form = new FormDataSet();
		form.setMultipart(true);
		form.getEntries().add(new FormData(Constants.TITLE_FIELD,
		new StringRepresentation(title)));	
		form.getEntries().add(new FormData(Constants.DESC_FIELD,
		new StringRepresentation(description)));
		
		Representation result = client.post(form);
		Media media = gson.fromJson(result.getText(), Media.class);

		// verify if resultant media has the passed attributes
		assertEquals(title, media.getTitle());
		assertEquals(description, media.getDescription());
	}

}