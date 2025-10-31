FROM node:18-alpine

WORKDIR /app

RUN apk add --no-cache libaio libnsl libc6-compat curl

COPY package*.json ./

RUN npm install --production

COPY . .

EXPOSE 3001

CMD ["npm", "start"]
