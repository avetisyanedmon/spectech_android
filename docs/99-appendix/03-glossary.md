# 03 — Glossary

Terms used in code and docs, with their Russian equivalents where the
backend or UI surfaces them.

## Domain terms

| Term | RU | Meaning |
|---|---|---|
| Order | Заказ | A rental request created by a customer |
| Bid | Ставка / Предложение | A contractor's proposal to fulfill an order with their equipment at a stated price |
| Equipment | Техника / Оборудование | A single piece of heavy machinery owned by a contractor |
| Customer | Заказчик | The user who creates an order |
| Contractor | Исполнитель / Поставщик | The user who submits bids and provides equipment |
| Deposit | Залог / Обеспечительный платёж | Performance security paid by a contractor before bidding |
| Marketplace | Маркетплейс / Объявления | Public list of open orders |
| Garage | Гараж | The contractor's personal inventory of equipment |
| Withdraw | Отозвать | A contractor cancels their own bid |
| Accept | Принять | The customer chooses a bid; binds the two parties |
| Republish | Опубликовать заново | Re-create an order from a deleted one |

## Pricing units (`PricingUnit`)

| Code | RU display | EN |
|---|---|---|
| `per_hour` | за час | per hour |
| `per_shift` | за смену | per shift |
| `per_m3` | за м³ | per cubic meter |
| `per_ton` | за тонну | per ton |
| `per_km` | за км | per kilometer |
| `per_ton_km` | за т·км | per ton-kilometer |
| `per_m3_km` | за м³·км | per cubic-meter-kilometer |
| `per_m2` | за м² | per square meter |
| `per_linear_m` | за погонный метр | per linear meter |

## Payment types

| Code | RU | EN |
|---|---|---|
| `cash` | Наличные | Cash |
| `nds` | С НДС | With VAT (Value Added Tax — Russian "НДС") |
| `usn` | УСН | Simplified Tax System ("Упрощённая Система Налогообложения") |

## Order statuses

| Code | RU | EN |
|---|---|---|
| `open` | Открыт | Open — awaiting bids |
| `pending` | В ожидании | Awaiting customer action |
| `accepted` | Принят | Customer accepted a bid |
| `in_progress` | В работе | Rental period started |
| `completed` | Завершён | Rental done |
| `cancelled` | Отменён | Cancelled by customer |
| `expired` | Истёк срок | Bidding window closed without acceptance |
| `closed` | Закрыт | Final state, no further changes |

## Deposit statuses

| Code | RU | EN |
|---|---|---|
| `pending` | В обработке | Created, not yet paid |
| `paid` | Оплачен | Confirmed by YooKassa |
| `failed` | Не удалось | Payment failed |
| `refund_pending` | Возврат в обработке | Refund initiated |
| `refunded` | Возвращён | Refund complete |
| `forfeited` | Удержан | Kept by the platform (dispute outcome) |

## Backend conventions

- All ID fields are UUIDs in the canonical 36-char hyphenated form.
- All dates are ISO-8601 with optional fractional seconds, in UTC.
- All money fields are JSON numbers (decimals). Backend uses
  `BigDecimal` server-side; client uses `BigDecimal` / `Decimal`.
- Phone numbers are E.164 strings: `+7XXXXXXXXXX` (10 subscriber digits).
- Snake-case keys on the wire; clients convert to camelCase for code.

## Russian equipment categories

Authoritative list in [03-domain/02-enums.md](../03-domain/02-enums.md).
The Russian "backend value" strings are sent verbatim in `POST /orders`
bodies — they are NOT for display; the UI titles come from `strings.xml`.

## Acronyms

| Acronym | Meaning |
|---|---|
| **NDS** | НДС — VAT (Налог на Добавленную Стоимость) |
| **USN** | УСН — simplified tax regime (Упрощённая Система Налогообложения) |
| **VIN** | Vehicle Identification Number |
| **OTP** | One-Time Password (SMS code) |
| **HMAC** | Hash-based Message Authentication Code |
| **JWT** | JSON Web Token (auth bearer) |
| **APNs** | Apple Push Notification service |
| **FCM** | Firebase Cloud Messaging |
| **SDK** | Software Development Kit |
| **E.164** | International phone number format standard |
| **DI** | Dependency Injection |
| **VM** | View Model |
| **MVVM** | Model-View-ViewModel |
| **ANR** | Application Not Responding |

## URLs

- Backend (production): `https://spectech-backoffice.onrender.com/api`
- Public website: `https://www.spectechweb.ru`
- Privacy Policy: `https://www.spectechweb.ru/privacy`
- Terms of Service: `https://www.spectechweb.ru/terms`
- Payment provider: YooKassa (`https://yookassa.ru`)
