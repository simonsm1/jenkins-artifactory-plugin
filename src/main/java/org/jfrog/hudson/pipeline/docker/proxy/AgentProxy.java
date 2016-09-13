package org.jfrog.hudson.pipeline.docker.proxy;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.ComputerListener;
import jenkins.model.Jenkins;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by romang on 8/14/16.
 */
@Extension
public class AgentProxy extends ComputerListener implements Serializable {

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        final boolean proxyEnabled = PluginsUtils.isProxyEnabled();
        final int port = PluginsUtils.getProxyPort();
        final String publicKey = PluginsUtils.getProxyPublicKey();
        final String privateKey = PluginsUtils.getProxyPrivateKey();
        if (proxyEnabled && c.getChannel() != null) {
            copyCertsToAgent(c, publicKey, privateKey);
            c.getChannel().call(new Callable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    DeProxy.init(port, publicKey, privateKey);
                    return true;
                }
            });
        }
        super.onOnline(c, listener);
    }

    private void copyCertsToAgent(Computer c, String publicKey, String privateKey) throws IOException, InterruptedException {
        if (!(c instanceof Jenkins.MasterComputer)) {
            FilePath remotePublicKey = new FilePath(c.getChannel(), publicKey);
            FilePath localPublicKey = new FilePath(new File(publicKey));
            localPublicKey.copyTo(remotePublicKey);

            FilePath remotePrivateKey = new FilePath(c.getChannel(), privateKey);
            FilePath localPrivateKey = new FilePath(new File(privateKey));
            localPrivateKey.copyTo(remotePrivateKey);
        }
    }
}