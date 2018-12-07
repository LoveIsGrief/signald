package io.finn.signald;

public class RunnableMain implements Runnable {
  private String socketPath;
  public RunnableMain(String s) {
    this.socketPath = s;
  }

  public void run() {
    Main.main(new String[]{this.socketPath});
  }
}
