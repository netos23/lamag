# Lama Docker
___

Dockerized Lama (provides `lamac`). Useful for ARM/Mac hosts where building Lama natively is problematic.

Build the image:

```bash
./scripts/docker_setup.sh
```

Run `lamac` against a file mounted from the working tree:

```bash
docker run --rm \
  --mount type=bind,src="$(pwd)/regression",dst=/mnt/work \
  -w /mnt/work \
  dlama:1.0.0 \
  /home/user/.opam/4.14.2/bin/lamac -i test001.lama
```
