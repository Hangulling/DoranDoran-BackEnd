import maintenance from '/public/maintenance.svg'

const MaintenancePage = () => {
  return (
    <div className="flex flex-col items-center justify-center h-full">
      <h2 className="text-[22px] font-semibold text-gray-400 mb-[46px] text-center">
        We’re updating things
        <br />
        behind the scenes!
      </h2>
      <img src={maintenance} />
      <p className="text-[14px] text-gray-400 mt-[15px] text-center">
        To serve you better,
        <br />
        we’re doing a quick maintenance check.
        <br />
        Thanks for your patience!
      </p>
    </div>
  )
}

export default MaintenancePage
