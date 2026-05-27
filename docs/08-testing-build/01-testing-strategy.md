# 01 — Testing Strategy

## Pyramid

```
                  E2E (a few)
              ──────────────────
             Integration (some)
        ───────────────────────────
                Unit (many)
```

Focus most effort on the bottom layer — pure JVM unit tests of domain
logic, view models, and the HMAC signer.

## Per-module test approach

| Module | Test framework | Examples |
|---|---|---|
| `core/domain` | JUnit 5 + Kotest assertions | enum normalization, `OrderFilters.matches`, lenient decoders |
| `core/network` | JUnit 5 + MockWebServer | HMAC payload bytes, retry behavior, error envelope decoding |
| `core/data` | JUnit 5 + MockK + Turbine | repository → API translation, event-bus emission |
| `core/platform` | Robolectric (only for storage) + JUnit | EncryptedSharedPreferences wrapping (lightweight) |
| `core/ui-kit` | Compose UI Test (Robolectric) | shared components like `LoadingStateView` |
| `features/*` | JUnit + Turbine (VMs); Compose UI Test (screens) | one VM test + one screen smoke test per feature |
| `app` | Espresso / Compose UI Test (instrumented) | nav graph happy path, deep-link routing |

## Critical things to test (priority order)

### 1. HMAC signing — must produce exact bytes iOS does

Highest-value test in the codebase. Pin the inputs and assert the signature
hex against a known-good value computed once by hand or from iOS logs.

```kotlin
class HmacSigningTest {
    @Test fun `signs example request matching iOS golden output`() {
        val secret = "66ff056ee8fa15b144a54ab472222b0a7534fe16286d1cfb893f6495fe65be96"
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        }
        val payload = "GET\n/api/orders?view=marketplace&limit=50&offset=0\n1736428800000\n0123456789abcdef0123456789abcdef\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val sig = mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
        // Run the same payload through iOS once to get the golden value; paste here.
        sig shouldBe "EXPECTED_HEX_FROM_IOS"
    }
}
```

### 2. JSON tolerance — `Order` / `Bid` decoding must not fail on unknown enums

```kotlin
@Test fun `Order decodes when equipmentCategory is unknown`() {
    val json = """{"id":"…","city":"X","equipment_category":"alien_machine","status":"open"}"""
    val order = Json.decodeFromString<Order>(json)
    order.equipmentCategory shouldBe null   // lenient: fell through
}

@Test fun `OrderStatus decodes legacy spellings`() {
    Json.decodeFromString<OrderStatus>("\"canceled\"") shouldBe OrderStatus.CANCELLED
    Json.decodeFromString<OrderStatus>("\"in progress\"") shouldBe OrderStatus.IN_PROGRESS
}
```

### 3. Phone normalization

```kotlin
@ParameterizedTest
@CsvSource(
    "9991234567,+79991234567",
    "89991234567,+79991234567",
    "79991234567,+79991234567",
    "+79991234567,+79991234567",
    "+7 (999) 123-45-67,+79991234567",
)
fun `normalizes Russian phone variants`(input: String, expected: String) {
    PhoneNormalizer().normalizeRussian(input) shouldBe expected
}

@Test fun `rejects phone numbers with wrong length`() {
    shouldThrow<ApiError> { PhoneNormalizer().normalizeRussian("123") }
}
```

### 4. ViewModel state flows

Use Turbine to assert the sequence of states a VM emits:

```kotlin
@Test fun `MarketplaceViewModel emits Loading then Loaded on success`() = runTest {
    val repo = mockk<OrdersRepository> {
        coEvery { fetchOrders(any(), any()) } returns listOf(sampleOrder())
    }
    val vm = MarketplaceViewModel(repo, sessionStore, AppEventBus())
    vm.state.test {
        awaitItem() shouldBe RemoteState.Idle
        vm.load()
        awaitItem() shouldBe RemoteState.Loading
        awaitItem().shouldBeInstanceOf<RemoteState.Loaded<List<Order>>>()
    }
}
```

### 5. Order filter matching

```kotlin
@Test fun `OrderFilters matches order with matching category`() {
    val filters = OrderFilters(categories = setOf(EquipmentCategory.DUMP_TRUCK))
    val order = sampleOrder(category = EquipmentCategory.DUMP_TRUCK)
    filters.matches(order) shouldBe true
}

@Test fun `OrderFilters excludes order with null category when filter is set`() {
    val filters = OrderFilters(categories = setOf(EquipmentCategory.DUMP_TRUCK))
    val order = sampleOrder(category = null)
    filters.matches(order) shouldBe false
}
```

### 6. Retry behavior

Use `MockWebServer` to assert that 502 retries happen the right number of
times with the right delays:

```kotlin
@Test fun `retries on 502 up to 2 times`() = runTest {
    val server = MockWebServer().apply {
        enqueue(MockResponse().setResponseCode(502))
        enqueue(MockResponse().setResponseCode(502))
        enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true,"data":[]}"""))
    }
    server.start()
    val client = buildApiClient(server.url("/").toString())
    val result = client.send<List<Order>>(OrdersApi.FetchOrders(OrderScope.MARKETPLACE, 50, 0, null))
    result.data shouldBe emptyList()
    server.requestCount shouldBe 3
    server.shutdown()
}
```

## Coverage targets

Per the project's existing standards (CLAUDE.md → testing.md):
- Domain: 90%+
- Network: 80%+
- Data (repositories): 80%+
- ViewModels: 70%+
- UI: smoke tests for each screen (binary coverage — does it render?)

## CI integration

```yaml
# .github/workflows/ci.yml
- name: Run unit tests
  run: ./gradlew test
- name: Generate coverage report
  run: ./gradlew jacocoTestReport
- name: Upload coverage
  uses: codecov/codecov-action@v4
```

See [03-ci-cd.md](03-ci-cd.md) for the full workflow.

## Compose UI testing

```kotlin
@get:Rule val composeRule = createComposeRule()

@Test fun `marketplace screen shows empty state when no orders`() {
    composeRule.setContent {
        SpecTechTheme {
            MarketplaceScreen(/* repo returns empty */)
        }
    }
    composeRule.onNodeWithText("No active marketplace orders are available.")
        .assertIsDisplayed()
}
```

## What NOT to test

- Trivial getters / setters (`val titleRes: Int`).
- Third-party libraries — trust Ktor, kotlinx-serialization, Compose.
- DI module wiring — the compile-time graph catches mistakes.
- Generated code (kotlinx-serialization companion objects, Hilt factories).

Focus tests on **behavior we've explicitly chosen to implement** — the
lenient decoders, the phone normalizer, the HMAC signer, the pagination
state machine.
