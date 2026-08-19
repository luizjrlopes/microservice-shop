module.exports = {
  default: {
    paths: ['features/**/*.feature'],
    requireModule: ['ts-node/register'],
    require: ['features/steps/**/*.ts'],
    publish: false,
  },
};
