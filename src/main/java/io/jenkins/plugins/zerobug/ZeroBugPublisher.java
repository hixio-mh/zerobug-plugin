package io.jenkins.plugins.zerobug;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import io.jenkins.plugins.zerobug.commons.Property;

public class ZeroBugPublisher extends Recorder {

	private final String token;
	private final boolean onlyBuildSuccess;

	@DataBoundConstructor
	public ZeroBugPublisher(final String token, final boolean onlyBuildSuccess) {
		this.token = token;
		this.onlyBuildSuccess = onlyBuildSuccess;
	}

	public String getToken() {
		return token;
	}

	public boolean isOnlyBuildSuccess() {
		return onlyBuildSuccess;
	}

	private String callServiceRest() {
		StringBuilder stringBuilder = new StringBuilder();

		try {
			URL urlConn = new URL(Property.getByKey("url.request"));
			URLConnection urlConnection = urlConn.openConnection();
			InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String inputLine = "";
			while ((inputLine = bufferedReader.readLine()) != null) {
				stringBuilder.append(inputLine);
			}
			bufferedReader.close();
			inputStreamReader.close();
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stringBuilder.toString();
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		if ((onlyBuildSuccess && Result.SUCCESS == build.getResult()) || !onlyBuildSuccess) {
			String response = callServiceRest();
			build.addAction(new ZeroBugAction(token, build.getUrl(), build));
			listener.getLogger().println(response);
			return true;
		}
		return false;
	}

	@Symbol("greet")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public FormValidation doCheckToken(@QueryParameter final String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error(Messages.ZeroBugPublisher_DescriptorImpl_errors_missingToken());
			}
			return FormValidation.ok();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.ZeroBugPublisher_DescriptorImpl_DisplayName();
		}
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

}
