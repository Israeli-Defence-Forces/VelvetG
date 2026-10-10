/** Real Minecraft class hierarchy, allocated without running the game constructor. */
public final class TestPlayer extends bew {
 public boolean alive=true,spectator,using;public zx held;
 public TestPlayer(){super(null,null,null,null);}
 @Override public String e_(){return "TestPlayer";}
 @Override public boolean ai(){return alive;}
 @Override public boolean v(){return spectator;}
 @Override public boolean bS(){return using;}
 @Override public zx bZ(){return held;}
}
