# ModuleTestingEnvironment

A test helper to instantiate a full headless TerasologyEngine instance

## Usage

Just write a test class that `extends ModuleTestingEnvironment`.

For complete docs please see the
[documentation on Github Pages](https://terasology.github.io/ModuleTestingEnvironment/org/terasology/moduletestingenvironment/ModuleTestingEnvironment.html)

For more examples see
[the test suite](https://github.com/terasology/ModuleTestingEnvironment/tree/master/src/test/java/org/terasology/moduletestingenvironment)

Here's an example taken from the test suite:

```java
public class MyModuleEngineTest extends ModuleTestingEnvironment {
    protected Logger logger = Logger.getLogger(MyModuleEngineTest.class.getName());
    protected EntityManager entityManager;

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("MyModule");
    }

    @Override
    public String getWorldGeneratorUri() {
        return "mymodule:mymodulesworldgenerator";
    }

    @Before
    public void beforeMyModuleTests() {
        entityManager = getHostContext().get(EntityManager.class);
        runUntil(()-> getHostContext().get(MyModuleReadySystem.class).isMyModuleReady());
    }
    
    @Test
    public void testExample() {
        WorldProvider worldProvider = getHostContext().get(WorldProvider.class);
        BlockManager blockManager = getHostContext().get(BlockManager.class);

        // create some clients (the library connects them automatically)
        Context clientContext1 = createClient();
        Context clientContext2 = createClient();

        // assert that both clients are known to the server
        EntityManager hostEntityManager = getHostContext().get(EntityManager.class);
        List<EntityRef> clientEntities = Lists.newArrayList(hostEntityManager.getEntitiesWith(ClientComponent.class));
        Assert.assertEquals(2, clientEntities.size());

        // send an event to a client's local player just for fun
        clientContext1.get(LocalPlayer.class).getClientEntity().send(new ResetCameraEvent());

        // wait for a chunk to be generated
        forceAndWaitForGeneration(Vector3i.zero());

        // set a block's type and immediately read it back
        worldProvider.setBlock(Vector3i.zero(), blockManager.getBlock("engine:air"));
        Assert.assertEquals("engine:air", worldProvider.getBlock(Vector3f.zero()).getURI().toString());
    }
}
```

## Receiving events

You can use a `TestEventReceiver` to inspect events fired against the engine context.

```java
TestEventReceiver receiver = new TestEventReceiver<>(context, DropItemEvent.class, (event, entity) -> {
  // do something with the event or entity
});
```

## Delay code

Conventionally, we use `while (condition)` to wait for delaying action. This can be done in MTE test by using `runWhile()` method. This runs the test enging while the condition is true.

```java
runWhile(() -> true);
```

Conversely, for running the enging _until_ some condition is true, use `runUntil()`

```java
runUntil(() -> false);
```


Check the JavaDoc and test suite for more usage examples.
