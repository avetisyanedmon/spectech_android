# 06 — Status & Category Mapping

Quick-lookup reference for how each enum maps to a UI label and (where
applicable) badge color.

## OrderStatus

| Wire | Label (EN) | Label (RU) | Background | Foreground |
|---|---|---|---|---|
| `open` | Open | Открыт | `#E3F2FD` | `#1565C0` |
| `pending` | Pending | В ожидании | `#FFF3E0` | `#E65100` |
| `accepted` | Accepted | Принят | `#E8F5E9` | `#2E7D32` |
| `in_progress` | In progress | В работе | `#E0F2F1` | `#00695C` |
| `completed` | Completed | Завершён | `#E0E0E0` | `#424242` |
| `cancelled` | Cancelled | Отменён | `#FFEBEE` | `#C62828` |
| `expired` | Expired | Истёк срок | `#FFEBEE` | `#C62828` |
| `closed` | Closed | Закрыт | `#E0E0E0` | `#424242` |

iOS source: `SpecTechIOS/Shared/Views/OrderStatusColor.swift` (read for exact
hex values; the table above matches iOS).

## DepositStatus → badge

| Wire | Badge label | Color |
|---|---|---|
| `pending` | "Deposit pending" | Amber `#FF9500` |
| `paid` | "Deposit paid" | Green `#34C759` |
| `failed` | "Deposit failed" | Red `#FF3B30` |
| `refund_pending` | "Refund in progress" | Amber `#FF9500` |
| `refunded` | "Refunded" | Gray `#8E8E93` |
| `forfeited` | "Forfeited" | Red `#FF3B30` |

Only `paid` and `pending` are visible on the equipment list card; the rest
appear only inside `DepositInfoSheet`.

## EquipmentCategory (titles & backend values)

See [03-domain/02-enums.md](../03-domain/02-enums.md) for the full list. The
authoritative source is `SpecTechIOS/Shared/Models/DomainModels.swift`.

## PricingUnit (titles & backend values)

| Wire | EN | RU | `backendCreateValue` |
|---|---|---|---|
| `per_hour` | per hour | за час | "за час" |
| `per_shift` | per shift | за смену | "за смену" |
| `per_m3` | per m³ | за м³ | "за м3" |
| `per_ton` | per ton | за тонну | "за тонну" |
| `per_km` | per km | за км | "за км" |
| `per_ton_km` | per t·km | за т·км | "за т*км" |
| `per_m3_km` | per m³·km | за м³·км | "за м3*км" |
| `per_m2` | per m² | за м² | "за м2" |
| `per_linear_m` | per linear m | за погонный метр | "за погонный метр" |

## PaymentType

| Wire | EN | RU | `backendCreateValue` |
|---|---|---|---|
| `cash` | Cash | Наличные | "наличные" |
| `nds` | VAT (NDS) | С НДС | "с ндс" |
| `usn` | USN | УСН | "усн" |

## UserRole

| Wire | EN | RU |
|---|---|---|
| `customer` | Customer | Заказчик |
| `contractor` | Contractor | Исполнитель |
| `admin` | Administrator | Администратор |

## Helper extension

```kotlin
@Composable
fun OrderStatus.localizedLabel(): String = stringResource(this.titleRes)

@Composable
fun OrderStatus.badgeColors(): Pair<Color, Color> = when (this) {
    OrderStatus.OPEN        -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    OrderStatus.PENDING     -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    OrderStatus.ACCEPTED    -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    OrderStatus.IN_PROGRESS -> Color(0xFFE0F2F1) to Color(0xFF00695C)
    OrderStatus.COMPLETED   -> Color(0xFFE0E0E0) to Color(0xFF424242)
    OrderStatus.CANCELLED   -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    OrderStatus.EXPIRED     -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    OrderStatus.CLOSED      -> Color(0xFFE0E0E0) to Color(0xFF424242)
}
```

Keep the colors here (single source of truth) and reference from
`OrderStatusBadge` and any other component that renders status.
