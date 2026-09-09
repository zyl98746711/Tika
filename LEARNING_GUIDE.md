# Tika RAG - 项目文档与学习路线

> 基于 Apache Tika 的 RAG（检索增强生成）文档解析与语义搜索服务

---

## 一、项目概览

### 这个项目做了什么？

一个完整的 RAG 文档处理管线：

```
上传文档 → 解析文本 → 智能分块 → 生成向量 → 存入向量数据库 → 语义搜索
```

### 技术栈

| 技术 | 用途 | 版本 |
|------|------|------|
| Spring Boot | Web 框架 | 3.4.4 |
| Apache Tika | 文档解析（PDF/Word/HTML等） | 3.1.0 |
| Ollama | 本地嵌入模型运行 | - |
| Qdrant | 向量数据库 | - |
| Java | 编程语言 | 21 |

### 外部依赖

- **Qdrant**：向量数据库，存储文档块的嵌入向量
- **Ollama**：本地 AI 模型服务，运行 `nomic-embed-text` 嵌入模型

---

## 二、项目目录结构

```
tika-rag/
├── src/
│   ├── main/
│   │   ├── java/com/tika/rag/
│   │   │   │
│   │   │   ├── TikaRagApplication.java          # Spring Boot 启动入口
│   │   │   │
│   │   │   ├── config/                          # 【配置层】应用配置
│   │   │   │   ├── AppConfig.java               #   Tika Bean 配置
│   │   │   │   ├── QdrantConfig.java            #   Qdrant 向量数据库配置 + REST 客户端
│   │   │   │   └── EmbeddingConfig.java         #   Ollama 嵌入模型配置 + REST 客户端
│   │   │   │
│   │   │   ├── parser/                          # 【解析层】文档解析
│   │   │   │   ├── DocumentParser.java          #   解析器接口（策略模式）
│   │   │   │   ├── TikaDocumentParser.java      #   Apache Tika 解析实现
│   │   │   │   └── FileTypeDetector.java        #   文件类型检测 + 格式校验
│   │   │   │
│   │   │   ├── chunker/                         # 【分块层】文本分块策略
│   │   │   │   ├── ChunkingStrategy.java        #   分块策略接口
│   │   │   │   ├── ChunkerRegistry.java         #   策略注册表（工厂模式）
│   │   │   │   ├── FixedSizeChunker.java        #   固定大小分块（按字符数）
│   │   │   │   ├── TokenBasedChunker.java       #   基于 Token 的分块
│   │   │   │   └── HierarchicalChunker.java     #   层级分块（按文档结构）
│   │   │   │
│   │   │   ├── embedding/                       # 【嵌入层】文本向量化
│   │   │   │   └── EmbeddingService.java        #   调用 Ollama 生成嵌入向量
│   │   │   │
│   │   │   ├── vectorstore/                     # 【存储层】向量数据库操作
│   │   │   │   └── VectorStoreService.java      #   Qdrant CRUD + 相似度搜索
│   │   │   │
│   │   │   ├── model/                           # 【模型层】数据模型
│   │   │   │   ├── ParsedDocument.java          #   解析后的文档（文本+元数据+格式）
│   │   │   │   ├── DocumentChunk.java           #   文档块（索引+文本+偏移量+token数）
│   │   │   │   ├── DocumentMetadata.java        #   文档元数据（标题/作者/日期等）
│   │   │   │   ├── SupportedFormat.java         #   支持的格式定义
│   │   │   │   ├── request/                     #   请求模型
│   │   │   │   │   ├── ChunkingRequest.java     #     分块请求参数
│   │   │   │   │   └── SearchRequest.java       #     搜索请求参数
│   │   │   │   └── response/                    #   响应模型
│   │   │   │       ├── ParseResponse.java       #     解析响应
│   │   │   │       ├── ChunkResponse.java       #     分块响应
│   │   │   │       ├── StoreResponse.java       #     入库响应
│   │   │   │       ├── SearchResponse.java      #     搜索结果响应
│   │   │   │       └── SupportedFormatsResponse.java
│   │   │   │
│   │   │   ├── service/                         # 【服务层】业务编排
│   │   │   │   ├── DocumentService.java         #   文档服务接口
│   │   │   │   ├── DocumentServiceImpl.java     #   文档服务实现（解析+分块）
│   │   │   │   └── SearchService.java           #   搜索服务（解析→分块→嵌入→存储/搜索）
│   │   │   │
│   │   │   ├── controller/                      # 【控制层】REST API
│   │   │   │   ├── DocumentController.java      #   文档解析/分块/格式 API
│   │   │   │   └── SearchController.java        #   入库/搜索/删除/统计 API
│   │   │   │
│   │   │   ├── exception/                       # 【异常层】统一异常处理
│   │   │   │   ├── GlobalExceptionHandler.java  #   全局异常处理器（@RestControllerAdvice）
│   │   │   │   ├── DocumentParseException.java  #   文档解析异常
│   │   │   │   └── UnsupportedFormatException.java
│   │   │   │
│   │   │   └── util/                            # 【工具层】辅助工具
│   │   │       └── TokenEstimator.java          #   Token 数量估算
│   │   │
│   │   └── resources/
│   │       ├── application.yml                  # 应用配置（端口、Tika、Qdrant、Ollama）
│   │       └── static/
│   │           └── index.html                   # 前端测试页面（5个标签页）
│   │
│   └── test/                                    # 测试代码
│       ├── java/com/tika/rag/
│       │   ├── chunker/                         # 分块器单元测试
│       │   ├── controller/                      # 控制器单元测试
│       │   ├── parser/                          # 解析器单元测试
│       │   └── service/                         # 集成测试
│       └── resources/test-documents/            # 测试文档样本
│
└── pom.xml                                      # Maven 依赖配置
```

---

## 三、核心数据流

### 3.1 文档入库流程

```
┌─────────────┐
│  用户上传文件 │  MultipartFile (PDF/Word/HTML/TXT/MD...)
└──────┬──────┘
       ▼
┌─────────────┐
│  Tika 解析   │  TikaDocumentParser.parse()
│  提取纯文本   │  → ParsedDocument { text, metadata, format }
└──────┬──────┘
       ▼
┌─────────────┐
│  智能分块     │  ChunkingStrategy.chunk()
│  切分为小块   │  → List<DocumentChunk> [chunk0, chunk1, chunk2, ...]
└──────┬──────┘
       ▼
┌─────────────┐
│  生成嵌入向量 │  EmbeddingService.embedBatch()
│  文本→向量   │  → List<float[768]> [vec0, vec1, vec2, ...]
└──────┬──────┘
       ▼
┌─────────────┐
│  存入 Qdrant │  VectorStoreService.storeChunks()
│  向量+元数据  │  → Point { id, vector, payload: {text, fileName, ...} }
└─────────────┘
```

### 3.2 语义搜索流程

```
┌─────────────┐
│  用户输入查询  │  "如何保护网络安全？"
└──────┬──────┘
       ▼
┌─────────────┐
│  查询转向量   │  EmbeddingService.embed()
│              │  → float[768]
└──────┬──────┘
       ▼
┌─────────────┐
│  向量搜索     │  VectorStoreService.search()
│  余弦相似度   │  → List<SearchResult> 按相似度降序
└──────┬──────┘
       ▼
┌─────────────┐
│  返回结果     │  最相关的文档块 + 分数 + 来源信息
└─────────────┘
```

---

## 四、API 接口一览

### 文档处理（DocumentController）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/documents/parse` | 解析文档，提取文本和元数据 |
| `POST` | `/api/v1/documents/chunk` | 解析并分块（不存储） |
| `GET`  | `/api/v1/documents/formats` | 获取支持的文档格式列表 |

### 向量存储与搜索（SearchController）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST`   | `/api/v1/documents/store` | 文档入库（解析→分块→嵌入→存储） |
| `POST`   | `/api/v1/documents/search` | 语义搜索 |
| `DELETE` | `/api/v1/documents/{id}` | 删除指定文档的所有向量 |
| `GET`    | `/api/v1/documents/stats` | 向量数据库统计信息 |

---

## 五、设计模式

项目中使用了多种经典设计模式：

| 模式 | 应用位置 | 说明 |
|------|----------|------|
| **策略模式** | `ChunkingStrategy` + 3个实现 | 分块算法可互换（固定大小/Token/层级） |
| **工厂模式** | `ChunkerRegistry` | 根据策略名称获取对应的分块器 |
| **接口隔离** | `DocumentParser` 接口 | 解析逻辑与实现解耦 |
| **依赖注入** | 所有 Service | 构造函数注入，方便测试 |
| **DTO 模式** | `record` 类 | 不可变数据传输对象 |
| **全局异常处理** | `GlobalExceptionHandler` | 统一错误响应格式 |

---

## 六、学习路线（建议顺序）

### 第一阶段：理解基础（1-2天）

```
目标：理解项目整体架构和 Spring Boot 基础
```

| 顺序 | 文件 | 学习重点 |
|------|------|----------|
| 1 | `TikaRagApplication.java` | Spring Boot 启动入口，`@SpringBootApplication` 注解 |
| 2 | `application.yml` | 配置管理，理解各配置项含义 |
| 3 | `pom.xml` | Maven 依赖管理，理解每个依赖的作用 |
| 4 | `model/` 下所有文件 | Java `record` 语法，数据模型设计 |

### 第二阶段：文档解析（2-3天）

```
目标：理解 Apache Tika 如何解析各种格式的文档
```

| 顺序 | 文件 | 学习重点 |
|------|------|----------|
| 5 | `parser/DocumentParser.java` | 接口设计，面向接口编程 |
| 6 | `parser/FileTypeDetector.java` | MIME 类型检测，文件类型识别 |
| 7 | `parser/TikaDocumentParser.java` | Tika API 使用，元数据提取 |
| 8 | `service/DocumentServiceImpl.java` | 服务层如何调用解析器 |
| 9 | `controller/DocumentController.java` | REST API 设计，`@RestController` |

### 第三阶段：分块策略（2-3天）

```
目标：理解策略模式和三种分块算法
```

| 顺序 | 文件 | 学习重点 |
|------|------|----------|
| 10 | `chunker/ChunkingStrategy.java` | 策略接口定义 |
| 11 | `chunker/FixedSizeChunker.java` | 固定大小分块（最简单，先理解概念） |
| 12 | `chunker/TokenBasedChunker.java` | Token 分块（理解 Token 概念） |
| 13 | `chunker/HierarchicalChunker.java` | 层级分块（最复杂，按文档结构） |
| 14 | `chunker/ChunkerRegistry.java` | 注册表/工厂模式 |
| 15 | `util/TokenEstimator.java` | Token 估算算法 |

### 第四阶段：向量存储与搜索（3-4天）

```
目标：理解 RAG 核心——嵌入向量和语义搜索
```

| 顺序 | 文件 | 学习重点 |
|------|------|----------|
| 16 | `config/EmbeddingConfig.java` | Ollama 配置，理解嵌入模型 |
| 17 | `embedding/EmbeddingService.java` | 嵌入原理，Ollama API 调用 |
| 18 | `config/QdrantConfig.java` | Qdrant 配置，向量数据库概念 |
| 19 | `vectorstore/VectorStoreService.java` | **核心文件**：向量存储、搜索、删除 |
| 20 | `service/SearchService.java` | RAG 完整流程编排 |
| 21 | `controller/SearchController.java` | 入库/搜索 API |

### 第五阶段：异常处理与前端（1-2天）

```
目标：理解全局异常处理和前后端交互
```

| 顺序 | 文件 | 学习重点 |
|------|------|----------|
| 22 | `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`，统一错误处理 |
| 23 | `exception/DocumentParseException.java` | 自定义异常 |
| 24 | `static/index.html` | 前端页面，Fetch API，拖放上传 |

### 第六阶段：测试（2-3天）

```
目标：理解 Spring Boot 测试体系
```

| 顺序 | 文件 | 学习重点 |
|------|------|----------|
| 25 | `chunker/*Test.java` | 纯单元测试（JUnit 5） |
| 26 | `parser/TikaDocumentParserTest.java` | 资源文件加载测试 |
| 27 | `controller/DocumentControllerTest.java` | `@WebMvcTest` + MockMvc + Mockito |
| 28 | `service/DocumentServiceIntegrationTest.java` | `@SpringBootTest` 集成测试 |

---

## 七、核心概念速查

### RAG（检索增强生成）

```
RAG = 检索 + 生成
本项目实现了"检索"部分：
  1. 将文档切成小块
  2. 每块文本转为向量（Embedding）
  3. 向量存入向量数据库（Qdrant）
  4. 查询时，用相似度搜索找到最相关的块
  5. 这些块可以作为上下文提供给 LLM 生成回答
```

### Embedding（嵌入/向量化）

```
文本 → 嵌入模型 → 向量（浮点数数组）

"机器学习" → nomic-embed-text → [0.12, -0.34, 0.56, ...] (768维)

关键特性：
  - 语义相近的文本，向量距离近
  - 支持跨语言（中英文）
  - 可以进行数学运算（相似度计算）
```

### 余弦相似度

```
cos(θ) = (A · B) / (|A| × |B|)

值域: [-1, 1]
  1.0  = 完全相同方向
  0.0  = 正交（无关）
 -1.0  = 完全相反方向

文本场景通常在 0.3 ~ 0.9 之间
```

### 分块策略对比

| 策略 | 原理 | 适用场景 |
|------|------|----------|
| `fixedSize` | 按固定字符数切分，相邻块有重叠 | 通用文档，简单快速 |
| `tokenBased` | 按 Token 数量切分 | 需要精确控制 Token 数的场景（如 LLM 上下文窗口） |
| `hierarchical` | 按文档结构（标题/段落）切分 | Markdown、HTML 等有结构的文档 |

---

## 八、运行与测试

```bash
# 运行测试
mvn test

# 启动应用（需要 Qdrant 和 Ollama 已运行）
mvn spring-boot:run

# 访问前端测试页面
# 浏览器打开 http://localhost:8080
```

### 外部服务启动

```bash
# WSL Debian 中启动 Qdrant
./qdrant

# WSL Debian 中启动 Ollama（安装后自动启动）
ollama serve
ollama pull nomic-embed-text    # 首次需要下载模型
```

---

## 九、扩展方向

学完本项目后，可以尝试以下扩展：

1. **接入 LLM**：将搜索结果作为上下文，调用 LLM（如 Ollama 的 llama3）生成回答
2. **混合搜索**：结合关键词搜索（BM25）和向量搜索，提高召回率
3. **重排序**：搜索后用 Cross-Encoder 模型对结果重排序
4. **多租户**：支持不同用户隔离的文档集合
5. **增量更新**：检测文档变更，只更新变化的块
6. **Spring AI 集成**：使用 Spring AI 框架简化嵌入和向量存储的集成
