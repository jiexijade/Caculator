## 多功能计算器各 Activity 代码逻辑设计

本文件在 `README.md` 功能要求的基础上，给出每个页面 / `Activity` 的**代码逻辑设计**与**数据结构规划**，便于在 Android Studio 中按图实现。

约定：
- 使用 Kotlin + `AppCompatActivity`。
- 每个功能一个独立 `Activity`，核心计算逻辑放到 `core`/`util` 层的单例或数据类中，UI 只负责收集输入、调用计算并显示结果。

---

### 1. 基本计算器 `CalculatorActivity`（`activity_calculator.xml`）

- **主要 UI 控件映射**
  - `TextView tvExpression`：当前输入/表达式（十进制模式下显示完整表达式，十六进制显示当前数）。
  - `TextView tvResult`：十进制结果显示。
  - `TextView tvHexResult`：十六进制结果预览/显示（仅 HEX 模式可见）。
  - `TextView tvMode`：标题栏右侧模式提示（“标准模式 / 十六进制模式”）。
  - `Button btnDecimalMode`：切换到十进制模式。
  - `Button btnHexMode`：切换到十六进制模式。
  - `GridLayout gridKeyboard`：键盘容器，内部所有 `Button` 统一通过代码遍历绑定点击事件（依赖 `android:text` 区分）。

- **核心状态字段**
  - `enum class Mode { DECIMAL, HEX }`：当前模式枚举。
  - `var currentMode: Mode`：记录当前是十进制还是十六进制。
  - 十六进制简单二元运算状态（只做“一个运算符、两个数”的形式）：
    - `var hexFirstOperand: Long?`：第一个十六进制操作数。
    - `var hexPendingOp: Char?`：当前等待执行的运算符（`+ - * /`）。

- **初始化流程**
  - `onCreate`：
    - `setContentView(R.layout.activity_calculator)`。
    - `findViewById` 绑定上述控件。
    - 给 `btnDecimalMode`、`btnHexMode` 设置 `setOnClickListener` 调用 `switchMode(...)`。
    - 遍历 `gridKeyboard` 中所有子视图：
      - 对每个 `Button` 设置点击事件：`handleKey(button.text.toString())`。
    - 默认调用 `switchMode(Mode.DECIMAL)`。

- **模式切换 `switchMode(mode: Mode)`**
  - 更新 `currentMode`。
  - 调用 `clearAll()` 重置表达式与结果。
  - DEC 模式：
    - `tvMode.text = "标准模式"`。
    - `tvHexResult.visibility = GONE`。
  - HEX 模式：
    - `tvMode.text = "十六进制模式"`。
    - `tvHexResult.visibility = VISIBLE`。

- **按键统一处理 `handleKey(label: String)`**
  - 特殊键：
    - `"AC"`：`clearAll()` → 将表达式和结果、HEX 状态全部置为初始。
    - `"DEL"`：`deleteLast()` → 删除 `tvExpression` 末尾一个字符，清空后置为 `"0"`。
    - `"="`：`evaluate()` → 根据当前模式调用不同求值逻辑。
  - 运算符键：
    - `"+", "-", "×", "÷", "%", "(", ")"` → 交给 `appendOperator(label)`。
  - 小数点、正负号：
    - `"."`、`"+/-"` → 交给 `handleSpecial(label)`。
  - 位移键：
    - `"<<"`、`">>"` → 交给 `handleShift(label)`。
  - 其它（数字和 A~F）：
    - 交给 `handleNumberOrHex(label)`，根据当前模式筛选合法字符。

- **十进制模式下的行为**
  - `handleNumberOrHex`：
    - 只允许 `0-9`，否则给出 Toast 提示“标准模式仅支持 0-9 数字”。
    - 使用 `appendToExpression(label)` 在末尾追加，若当前为 `"0"` 则直接替换。
  - `appendOperator`：
    - 将 `"×"`、`"÷"` 映射为 `"*"`、`"/"`，其他运算符保持不变。
    - 直接调用 `appendToExpression(realOp)`，不做复杂语法检查（由表达式解析器容错）。
  - `handleSpecial`：
    - `"."`：直接追加小数点（可按需要额外限制“一个数字不能有多个点”）。
    - `"+/-"`：`toggleSign()`，对当前 `tvExpression` 整体加负号或去掉负号。
  - `evaluateDecimal()`：
    - 获取 `expr = tvExpression.text.toString()`。
    - 调用 `ExpressionEvaluator.evaluate(expr)`：
      - 支持：`+ - * / % ()` 以及带小数的数字。
      - 用 tokenizer + 中缀转后缀（逆波兰）+ 栈实现。
      - 处理一元正负号（例如 `-2+3`）。
    - 成功则 `tvResult.text = value.toString()`，失败抛异常时 Toast“表达式错误”。

- **十六进制模式下的行为**
  - `handleNumberOrHex`：
    - 接受 `0-9 A-F`（不区分大小写），统一转为大写后显示。
    - 更新 `tvExpression`，同时调用 `updateHexPreview()`：
      - 将当前输入解析为十六进制 `Long`，在 `tvHexResult` 中显示 `"HEX: XX"`，并可根据需要同步十进制结果。
  - 小数点 `"."`：
    - 在 HEX 模式下不允许，弹 Toast“十六进制不支持小数点”。
  - `appendOperator`：
    - 在 HEX 模式下只允许 `+ - × ÷` 几种简单运算：
      - 调 `prepareHexBinaryOp(opLabel)`：
        - 将当前输入解析为十六进制 `Long` 存入 `hexFirstOperand`。
        - 把运算符转为 `Char`（`'*', '/', '+', '-'`）存入 `hexPendingOp`。
        - 清空 `tvExpression` 为 `"0"`，等待第二个操作数。
    - 其它运算符（如 `%`、括号）弹 Toast“当前模式下不支持该运算符”。
  - 位移 `handleShift("<<"/">>")`：
    - 仅在 HEX 模式下可用：
      - 解析当前输入为十六进制整数 `value`。
      - `<<` → `value shl 1`；`>>` → `value shr 1`。
      - 用结果更新 `tvExpression`（十六进制字符串）和 `tvHexResult`。
  - `evaluateHex()`：
    - 获取当前输入作为第二个操作数 `second`（十六进制 `Long`）。
    - 若 `hexFirstOperand` 或 `hexPendingOp` 空：
      - 表示只有一个数，则仅更新 `tvResult`（十进制）和 `tvHexResult`（十六进制），不做二元运算。
    - 否则按 `+ - * /` 执行运算：
      - 除法时检查被除数是否为 0。
      - 结果 `result: Long` 转为：
        - 十进制：`tvResult.text = result.toString()`。
        - 十六进制：`tvHexResult.text = "HEX: ${result.toString(16).uppercase()}"`。
        - 同时把 `tvExpression` 更新为结果的十六进制。
      - 运算结束后清空 `hexFirstOperand` 与 `hexPendingOp`，方便继续新的运算。

- **表达式求值工具 `ExpressionEvaluator`（独立对象）**
  - 提供 `fun evaluate(expr: String): Double`。
  - 内部步骤：
    1. `tokenize(expr)`：字符串 → 记号列表（数字、运算符、括号），支持带符号数字。
    2. `infixToPostfix(tokens)`：中缀表达式 → 后缀表达式（逆波兰）。
    3. `evalPostfix(tokens)`：使用栈计算逆波兰表达式结果。
  - 支持运算符优先级：`* / %` 高于 `+ -`，支持括号改变优先级。

---

### 2. 复数科学计算器 `ComplexCalculatorActivity`（`activity_complex_calculator.xml`）

- **UI 控件设计（建议）**
  - 输入两个复数 \((a + bi)\)、\((c + di)\)：
    - `EditText etA, etB`：分别输入第一个复数的实部、虚部。
    - `EditText etC, etD`：第二个复数的实部、虚部。
  - 运算按钮：
    - `Button btnComplexAdd, btnComplexSub, btnComplexMul, btnComplexDiv`。
  - 显示：
    - `TextView tvComplexResult`：显示计算结果，如 `"3.0 + 4.0i"`。

- **数据模型 `Complex`**
  - `data class Complex(val re: Double, val im: Double)`：
    - `operator fun plus(other: Complex): Complex`
    - `operator fun minus(other: Complex): Complex`
    - `operator fun times(other: Complex): Complex`
    - `operator fun div(other: Complex): Complex`（注意分母不为 0）。
    - `override fun toString()`：负责格式化为 `"a + bi"` / `"a - bi"`，保留一定小数位。

- **Activity 逻辑**
  - 读取输入：
    - 从 `etA/B/C/D` 取字符串，转换为 `Double`：
      - 可设空字符串视为 0，或者要求必填时检测为空给出 Toast。
  - 构造 `val z1 = Complex(a, b)`，`val z2 = Complex(c, d)`。
  - 各运算按钮的点击事件：
    - `btnComplexAdd`：`val result = z1 + z2`。
    - `btnComplexSub`：`val result = z1 - z2`。
    - `btnComplexMul`：`val result = z1 * z2`。
    - `btnComplexDiv`：
      - 检查 `z2.re == 0 && z2.im == 0` 时，Toast“除数不能为 0”。
      - 否则 `val result = z1 / z2`。
  - 结果显示：
    - `tvComplexResult.text = result.toString()`。

---

### 3. 房贷 / 车贷计算器 `LoanCalculatorActivity`（`activity_loan_calculator.xml`）

- **UI 控件设计（建议）**
  - 输入区域：
    - `EditText etLoanAmount`：贷款总额（单位：元）。
    - `EditText etYears`：贷款年限（单位：年）。
    - `EditText etRate`：年利率（百分比，例如 4.2 表示 4.2%）。
    - `Spinner spLoanType`：贷款类型（房贷 / 车贷，可只做展示，不影响公式）。
    - `RadioGroup rgRepayType`：
      - `RadioButton rbEqualPrincipalInterest`：等额本息。
      - `RadioButton rbEqualPrincipal`：等额本金。
  - 操作与结果：
    - `Button btnLoanCalc`：执行计算。
    - `TextView tvMonthlyPayment`：月供（或等额本息时固定月供；等额本金时首月月供）。
    - `TextView tvTotalInterest`：总利息。
    - `TextView tvTotalPayment`：本息合计。

- **核心数据结构**
  - `data class LoanResult(val monthlyPayment: Double, val totalInterest: Double, val totalPayment: Double)`
  - `object LoanCalculator`：
    - `fun equalPrincipalInterest(principal: Double, years: Int, annualRatePercent: Double): LoanResult`
    - `fun equalPrincipal(principal: Double, years: Int, annualRatePercent: Double): LoanResult`

- **等额本息公式实现**
  - 定义：
    - `P`：贷款本金。
    - `i`：月利率 = 年利率 / 12 / 100。
    - `n`：总期数 = 年限 × 12。
  - 公式：
    \[
      M = P \times \frac{i(1+i)^n}{(1+i)^n - 1}
    \]
    其中 \(M\) 为每月还款额（固定）。
  - 实现步骤：
    - 计算 `i`、`n`。
    - 用 `Math.pow(1 + i, n.toDouble())` 得到 \((1+i)^n\)。
    - 按公式算出 `M`。
    - 总还款 `totalPayment = M * n`。
    - 总利息 `totalInterest = totalPayment - principal`。
    - 返回 `LoanResult(M, totalInterest, totalPayment)`。

- **等额本金公式实现**
  - 定义：
    - 每期应还本金：`principal / n`。
    - 第 `k` 期的剩余本金：`principal - (k - 1) * principal / n`。
    - 第 `k` 期利息：`remaining * i`。
    - 第 `k` 期月供：`principal/n + interest_k`。
  - 实现步骤：
    - 计算 `i`、`n`。
    - 循环 `k` 从 1 到 `n`：
      - 计算剩余本金、当期利息、当期月供。
      - 累加总利息、总还款。
      - 记录首月月供 `firstMonthPayment`，用于展示。
    - 返回 `LoanResult(firstMonthPayment, totalInterest, totalPayment)`。

- **Activity 逻辑**
  - `btnLoanCalc` 点击：
    - 读取输入：贷款总额、年限、年利率，转换为 `Double`/`Int`，并校验 > 0。
    - 根据 `RadioGroup` 判断是等额本金还是等额本息：
      - 调用对应的 `LoanCalculator` 方法。
    - 使用 `String.format(Locale.CHINA, "%.2f", value)` 显示两位小数。
    - 把结果填充到三个 `TextView` 中。
    - 若输入为空或格式错误，使用 Toast 提示“请输入合法数字”。

---

### 4. 汇率计算器 `CurrencyCalculatorActivity`（`activity_currency_calculator.xml`）

- **UI 控件设计（建议）**
  - 选择币种：
    - `Spinner spFromCurrency`：源币种（如 CNY, USD, EUR, JPY...）。
    - `Spinner spToCurrency`：目标币种。
  - 输入与结果：
    - `EditText etCurrencyAmount`：输入原金额。
    - `TextView tvCurrencyResult`：显示转换结果。
  - 操作：
    - `Button btnCurrencyConvert`：执行换算。
    - 可选：`Button/菜单项` 用于手动更新汇率。

- **本地汇率数据结构（不使用网络）**
  - 约定：所有汇率以“相对人民币 CNY”存储。
  - `object ExchangeRates`：
    - `private val ratesToCny: MutableMap<String, Double>` 例如：
      - `"CNY" -> 1.0`
      - `"USD" -> 7.20`
      - `"EUR" -> 7.80`
      - `"JPY" -> 0.05`
    - `fun convert(amount: Double, from: String, to: String): Double`：
      - `amountInCny = amount * ratesToCny[from]`。
      - `result = amountInCny / ratesToCny[to]`。
    - `fun updateRate(currency: String, rateToCny: Double)`：手动更新某币种相对人民币的汇率。

- **Activity 逻辑**
  - `btnCurrencyConvert` 点击：
    - 读取输入金额与 `Spinner` 中的两个币种代码。
    - 校验：金额非空且为正数。
    - 调用 `ExchangeRates.convert(amount, from, to)` 获得结果。
    - 格式化结果（保留 2–4 位小数）显示在 `tvCurrencyResult`。
  - 手动更新汇率（可选）：
    - 例如长按某个币种或点击“设置”按钮弹出对话框：
      - 输入新的“1 单位币种 = ? CNY”数值。
      - 调用 `ExchangeRates.updateRate(code, newRate)`。

---

### 5. 单位换算各 Activity

#### 5.1 长度换算 `LengthConverterActivity`（`activity_length_converter.xml`）

- **UI 控件设计**
  - `Spinner spLengthFrom`、`spLengthTo`：选择源单位和目标单位（m, km, cm, mm, inch, ft 等）。
  - `EditText etLengthValue`：输入数值。
  - `TextView tvLengthResult`：显示转换结果。
  - `Button btnLengthConvert`：执行转换。

- **通用计算工具 `LengthConverter`**
  - 统一以“米 m”为基础单位：
  - `object LengthConverter`：
    - `private val factors = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "inch" to 0.0254, "ft" to 0.3048)`
    - `fun convert(value: Double, from: String, to: String): Double`：
      - `inMeters = value * factors[from]`。
      - `result = inMeters / factors[to]`。

- **Activity 逻辑**
  - 读取输入值与单位，校验数值合法。
  - 调用 `LengthConverter.convert(value, from, to)`。
  - 把结果格式化显示在 `tvLengthResult`。

#### 5.2 重量换算 `WeightConverterActivity`（`activity_weight_converter.xml`）

- **UI 控件设计**
  - 类似长度换算：
    - `Spinner spWeightFrom`, `spWeightTo`。
    - `EditText etWeightValue`。
    - `TextView tvWeightResult`。
    - `Button btnWeightConvert`。

- **工具类 `WeightConverter`（以千克 kg 为基础单位）**
  - `object WeightConverter`：
    - `private val factors = mapOf("kg" to 1.0, "g" to 0.001, "t" to 1000.0, "lb" to 0.45359237)`。
    - `fun convert(value: Double, from: String, to: String): Double`：
      - `inKg = value * factors[from]`。
      - `result = inKg / factors[to]`。

- **Activity 逻辑**
  - 与 `LengthConverterActivity` 类似，调用 `WeightConverter.convert` 并显示结果。

#### 5.3 速度换算 `SpeedConverterActivity`（`activity_speed_converter.xml`）

- **UI 控件设计**
  - `Spinner spSpeedFrom`, `spSpeedTo`。
  - `EditText etSpeedValue`。
  - `TextView tvSpeedResult`。
  - `Button btnSpeedConvert`。

- **工具类 `SpeedConverter`（以 m/s 为基础单位）**
  - `object SpeedConverter`：
    - `private val factors = mapOf("m/s" to 1.0, "km/h" to 1000.0 / 3600.0, "mph" to 0.44704)`。
    - `fun convert(value: Double, from: String, to: String): Double`：
      - `inMs = value * factors[from]`。
      - `result = inMs / factors[to]`。

#### 5.4 数据单位换算 `DataConverterActivity`（可选，新建布局）

- **UI 控件设计**
  - `Spinner spDataFrom`, `spDataTo`。
  - `EditText etDataValue`。
  - `TextView tvDataResult`。
  - `Button btnDataConvert`。

- **工具类 `DataUnitConverter`（以 Byte 为基础单位）**
  - `object DataUnitConverter`：
    - `private val factors = mapOf("B" to 1.0, "KB" to 1024.0, "MB" to 1024.0 * 1024, "GB" to 1024.0 * 1024 * 1024)`。
    - `fun convert(value: Double, from: String, to: String): Double`：
      - `inBytes = value * factors[from]`。
      - `result = inBytes / factors[to]`。

---

### 6. 自定义扩展功能（可选实现）

- **历史记录模块**
  - 数据结构：
    - `data class HistoryItem(val module: String, val input: String, val result: String, val timestamp: Long)`。
  - 管理类：
    - `object HistoryManager { val items = mutableListOf<HistoryItem>() }`。
  - 使用方式：
    - 在每个计算 Activity 成功计算后：
      - `HistoryManager.items.add(HistoryItem("Calculator", expr, result, System.currentTimeMillis()))`。
    - 新建 `HistoryActivity` + `RecyclerView` 展示 `items`。

- **主菜单 / 导航**
  - `MainActivity` 上放入口按钮：
    - “基础计算器”、“复数计算器”、“房贷车贷”、“汇率换算”、“单位换算”等。
  - 点击后使用：
    - `startActivity(Intent(this, CalculatorActivity::class.java))` 等跳转到对应界面。

---

### 7. 建议的包结构

- `ui.calculator.CalculatorActivity`
- `ui.calculator.ComplexCalculatorActivity`
- `ui.loan.LoanCalculatorActivity`
- `ui.currency.CurrencyCalculatorActivity`
- `ui.converter.LengthConverterActivity` / `SpeedConverterActivity` / `WeightConverterActivity` / `DataConverterActivity`
- `core.math.Complex` / `core.math.ExpressionEvaluator`
- `core.loan.LoanCalculator`
- `core.currency.ExchangeRates`
- `core.converter.LengthConverter` / `WeightConverter` / `SpeedConverter` / `DataUnitConverter`

你可以根据本设计文档，在各 Activity 中按步骤实现控件绑定、输入校验和调用对应工具类的计算方法，即可完成课程要求的 Android 多功能计算器应用。

