### Java upgrades { data-toc-label="Java upgrades" }

The Java upgrade recipes migrate between supported Java releases. Each recipe applies OpenRewrite's migration for its target release without rerunning earlier upgrade recipes.

| From | To |
|------|----|
| 5    | 6  |
| 6    | 7  |
| 7    | 8  |
| 8    | 11 |
| 11   | 17 |
| 17   | 21 |
| 21   | 25 |

Run an upgrade by specifying its source and target releases:

```bash
allwrite run java/upgrade 17 21
```
