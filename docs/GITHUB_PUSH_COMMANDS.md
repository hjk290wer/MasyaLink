# First push commands

Run these commands in the project root.

```bash
git init
git add .
git commit -m "Initial MasyaLink Android baseline"
git branch -M main
git remote add origin https://github.com/hjk290wer/MasyaLink.git
git push -u origin main
```

If Git asks for login/password, use GitHub Desktop or a GitHub Personal Access Token. Do not paste tokens into ChatGPT.

## After successful GitHub Actions build

Create a stable tag:

```bash
git tag v1.2-reply-delete-baseline
git push origin v1.2-reply-delete-baseline
```
