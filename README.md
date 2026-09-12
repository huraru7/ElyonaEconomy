# ElyonaEconomy

Elyona World の通貨(Cred)を管理するプラグイン。送金・残高確認・管理者向け残高操作を提供する。

## 主な機能

- 所持Credの確認
- プレイヤー間送金(取引税あり、オーバーフロー対策済み)
- 管理者向け残高操作(付与・減算・設定)

## コマンド

| コマンド | 説明 | 権限 |
|---|---|---|
| `/balance` (`/bal`) | 所持Credを確認する | なし |
| `/pay <player> <amount>` | 他のプレイヤーにCredを送金する(税あり) | なし |
| `/eco <give\|take\|set> <player> <amount>` | 残高を管理する | `elyona.admin` |

## 依存関係

- Paper 1.21.1
- [ElyonaCore](https://github.com/huraru7/ElyonaCore)

## ビルド

```
./gradlew build
```

Java 21 / Paper 1.21.1 (paperweight userdev) を使用。
