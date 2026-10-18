package me.eldodebug.soar.discord.ipc.entities.pipe;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import me.eldodebug.soar.discord.DiscordRPC;
import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.entities.*;

public final class DiscordChecks {
  static class FakePipe extends Pipe {
    int closes;
    boolean fail;

    FakePipe(IPCClient client) {
      super(client, new HashMap<String, Callback>());
    }

    public Packet read() {
      return null;
    }

    public void write(byte[] bytes) {}

    public void close() throws IOException {
      closes++;
      if (fail) throw new IOException("synthetic closed pipe");
      setStatus(PipeStatus.CLOSED);
    }
  }

  public static void main(String[] args) throws Exception {
    new DiscordRPC().stop();
    IPCClient client = new IPCClient(1);
    client.close();
    Field field = IPCClient.class.getDeclaredField("pipe");
    field.setAccessible(true);
    FakePipe pipe = new FakePipe(client);
    pipe.setStatus(PipeStatus.CONNECTED);
    field.set(client, pipe);
    client.close();
    if (pipe.closes != 1) throw new AssertionError("Connected pipe not closed");
    client.close();
    if (pipe.closes != 1) throw new AssertionError("Repeated close not idempotent");
    pipe.fail = true;
    pipe.setStatus(PipeStatus.CONNECTED);
    client.close();
    if (pipe.closes != 2) throw new AssertionError("IO failure not handled");
    System.out.println(
        "PASS 5 Discord null-stop, uninitialized-close, connected-close, repeated-close and"
            + " IOException checks (IPC mocked)");
  }
}
